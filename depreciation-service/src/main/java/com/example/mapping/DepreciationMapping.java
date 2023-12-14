package com.example.mapping;

import com.example.client.DepreciationServiceClient;
import com.example.dto.request.DepreciationRequest;
import com.example.dto.response.*;
import com.example.model.Depreciation;
import com.example.model.DepreciationHistory;
import com.example.service.DepreciationHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DepreciationMapping {
    private final DepreciationServiceClient depreciationServiceClient;
    private final DepreciationHistoryService depreciationHistoryService;
    private final CommonMapping commonMapping;

    //Hàm nhận request và thực hiện tính khấu hao và lưu lịch sử
    public Depreciation requestToEntity(DepreciationRequest depreciationRequest, Object object) throws ParseException {
        Depreciation depreciation = new Depreciation();
        depreciation.setActive(true);
        depreciation.setStatus(1);
        depreciation.setDeptId(depreciationRequest.getDeptId());
        depreciation.setAssetId(depreciationRequest.getAssetId());
        depreciation.setFromDate(new Date());
        AssetResponse assetResponse = depreciationServiceClient.fetchAsset(depreciation.getAssetId());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        depreciation.setAssetTypeId(assetResponse.getAssetTypeId());
        if(assetResponse.getExpDate() == null)
            depreciation.setExpDate(Date.from(LocalDate.now().plusMonths(assetResponse.getAmountOfYear()).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        else
            depreciation.setExpDate(dateFormat.parse(assetResponse.getExpDate()));
        //Ngưng khấu hao đợt trước và gán khấu hao từ sau ngày kết thúc
        if(object != null){
            //Ngày khấu hao cuối cùng
            Date lDate = dateFormat.parse(((Object[])object)[1].toString());
            //Giá trị đã khấu hao
            Double valuePrev = Double.valueOf(((Object[])object)[2].toString());
            depreciation.setValuePerMonth(commonMapping.calculatorDepreciationPerMonth(assetResponse,valuePrev, dateFormat.format(lDate)));
            LocalDate localDate = lDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1);
            depreciation.setFromDate(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        else depreciation.setValuePerMonth(assetResponse.getPrice()/assetResponse.getAmountOfYear());
        depreciation.setUserId(depreciationRequest.getUserId());
        depreciation.setCreateAt(new Date());
        return depreciation;
    }

    public Depreciation updateDepreciation(Depreciation depreciation) throws ParseException {
        Date endDate = new Date();
        depreciation.setToDate(endDate);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int fMonth = LocalDate.of(depreciation.getFromDate().getYear()+1900,depreciation.getFromDate().getMonth()+1,01).lengthOfMonth();
        int eMonth = LocalDate.of(endDate.getYear()+1900,endDate.getMonth()+1,01).lengthOfMonth();
        int exMonth = LocalDate.of(depreciation.getExpDate().getYear()+1900,depreciation.getExpDate().getMonth()+1,01).lengthOfMonth();
        Date sDate = dateFormat.parse((endDate.getYear()+1900)+"-"+(endDate.getMonth()+1)+"-01");
        int amountMonth = 0;
        //Tính lại giá trị đã khấu hao
        Double value = depreciationHistoryService.totalValueDepreciationByDepreciationId(depreciation.getId(),endDate.getMonth()+1,endDate.getYear()+1900);
        if(value ==null)
            value = 0.0;
        Double valueInMonth = 0.0;
        if(depreciation.getExpDate().before(sDate)){
            valueInMonth= 0.0;
            amountMonth = (depreciation.getFromDate().getDate() > fMonth/2 ? 0 : 1)
                    + (11 - depreciation.getFromDate().getMonth())
                    + (depreciation.getExpDate().getYear() - depreciation.getFromDate().getYear() -1)*12
                    + (depreciation.getExpDate().getMonth())
                    + (depreciation.getExpDate().getDate() > exMonth/2 ? 1: 0);
        }else if(depreciation.getExpDate().after(sDate)&&depreciation.getExpDate().before(endDate)){
            valueInMonth = (Double.valueOf(depreciation.getExpDate().getDate() - sDate.getDate()+1)/eMonth) * depreciation.getValuePerMonth();
            amountMonth = (depreciation.getFromDate().getDate() > fMonth/2 ? 0 : 1)
                    + (11 - depreciation.getFromDate().getMonth())
                    + (depreciation.getExpDate().getYear() - depreciation.getFromDate().getYear() -1)*12
                    + (depreciation.getExpDate().getMonth())
                    + (depreciation.getExpDate().getDate() > exMonth/2 ? 1: 0);
        }else if(depreciation.getExpDate().after(endDate)){
            valueInMonth = (Double.valueOf(endDate.getDate() - sDate.getDate()+1)/eMonth) * depreciation.getValuePerMonth();
            amountMonth = (depreciation.getFromDate().getDate() > fMonth/2 ? 0 : 1)
                    + (11 - depreciation.getFromDate().getMonth())
                    + (endDate.getYear() - depreciation.getFromDate().getYear() -1)*12
                    + (endDate.getMonth())
                    + (endDate.getDate() > eMonth/2 ? 1: 0);
        }
        depreciation.setAmountMonth(amountMonth);
        depreciation.setValueDepreciation(value+valueInMonth);
        depreciation.setStatus(2);
        //Lưu thông tin lịch sử khấu hao
        DepreciationHistory depreciationHistory = new DepreciationHistory();
        depreciationHistory.setCreateAt(new Date());
        depreciationHistory.setMonth(endDate.getMonth()+1);
        depreciationHistory.setYear(endDate.getYear()+1900);
        depreciationHistory.setDepreciation(depreciation);
        depreciationHistory.setAssetId(depreciation.getAssetId());
        depreciationHistory.setAssetTypeId(depreciation.getAssetTypeId());
        depreciationHistory.setValue(valueInMonth);
        depreciationHistoryService.saveDepreciationHistory(depreciationHistory);
        return depreciation;
    }

}
