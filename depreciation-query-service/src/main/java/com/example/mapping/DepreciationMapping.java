package com.example.mapping;

import com.example.client.DepreciationServiceClient;
import com.example.dto.request.DepreciationRequest;
import com.example.dto.response.AssetResponse;
import com.example.dto.response.DepreciationByAssetResponse;
import com.example.dto.response.DepreciationHistoryByDepreciation;
import com.example.dto.response.UserResponse;
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

//    public DepreciationResponse EntityToResponse(Depreciation depreciation){
//        DepreciationResponse depreciationResponse = new DepreciationResponse();
//        AssetResponse assetResponse = depreciationServiceClient.fetchAsset(depreciation.getAssetId());
//        depreciationResponse.setAssetResponse(assetResponse);
//        depreciationResponse.setId(depreciation.getId());
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        depreciationResponse.setFromDate(dateFormat.format(depreciation.getFromDate()));
//        if(depreciation.getToDate()==null){
//            depreciationResponse.setToDate(null);
//            depreciationResponse.setAmountMonth(new Date().getMonth() - depreciation.getFromDate().getMonth());
//            depreciationResponse.setValueDepreciation(commonMapping.calculatorDepreciation(assetResponse, dateFormat.format(depreciation.getFromDate()),dateFormat.format(new Date()), ));
////            depreciationResponse.setValueDepreciation(depreciationServiceClient.getDepreciationValue(depreciation.getAssetId(),dateFormat.format(depreciation.getFromDate()) ,dateFormat.format(new Date())));
//        }
//        else{
//            depreciationResponse.setToDate(dateFormat.format(depreciation.getToDate()));
//            depreciationResponse.setAmountMonth(depreciation.getToDate().getMonth() - depreciation.getFromDate().getMonth());
//            depreciationResponse.setValueDepreciation(depreciation.getValueDepreciation());
//        }
//        depreciationResponse.setCreateAt(dateFormat.format(depreciation.getCreateAt()));
//        depreciationResponse.setActive(depreciation.isActive());
//        depreciationResponse.setUserResponse(depreciationServiceClient.fetchUser(depreciation.getUserId()));
//        return depreciationResponse;
//    }

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

    public DepreciationByAssetResponse getDepreciationAssetResponse(Long assetId, List<Depreciation> lDepreciation) throws ParseException {
        DepreciationByAssetResponse depreciationByAssetResponse = new DepreciationByAssetResponse();
        LocalDate localDate = LocalDate.now();
        int amountDate = 0;
        Double valuePre = 0.0;
        Double valuePrev = depreciationHistoryService.totalValueDepreciationByAssetId(assetId,new Date().getMonth()+1, new Date().getYear()+1900);
        //Khởi tạo ngày đầu tháng và hôm nay
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date sDate = dateFormat.parse(localDate.getYear()+"-"+localDate.getMonthValue()+"-01");
        Date today = new Date();
        AssetResponse assetResponse = depreciationServiceClient.fetchAsset(assetId);
        Date expDate = dateFormat.parse(assetResponse.getExpDate());
        List<DepreciationByAssetResponse.DepreciationAssetHistory> list = new ArrayList<>();
        for(Depreciation depreciation: lDepreciation){
            List<DepreciationHistoryByDepreciation> depreciationList = getDepreciationHistoryByDepreciation(depreciation);
            UserResponse userResponse = depreciationServiceClient.fetchUser(depreciation.getUserId());
            Double value = 0.0;
            if(depreciation.getToDate() == null){
                Object object = depreciationHistoryService.getValueHistoryByDepreciation(localDate.getMonthValue(), localDate.getYear(), depreciation.getId());
                value += object != null ? Double.valueOf(((Object[])object)[1].toString()): 0.0;
                //Kiểm tra tài sản còn khấu hao hay không
                if(expDate.after(sDate)&&expDate.before(today)){
                    //Khấu hao được tính từ ngày đầu đến ngày kết thúc
                    amountDate = expDate.getDate()-sDate.getDate()+1;
                    valuePre = (Double.valueOf(amountDate)/localDate.lengthOfMonth())*depreciation.getValuePerMonth();
                }else if(expDate.before(sDate)){
                    //Khấu hao đã kết thúc
                    valuePre = 0.0;
                    amountDate = 0;
                }else {
                    //Khấu hao vẫn chưa hết
                    amountDate = today.getDate()-sDate.getDate()+1;
                    valuePre = (Double.valueOf(amountDate)/localDate.lengthOfMonth())*depreciation.getValuePerMonth();
                }
                value+=valuePre;
            }
            String toDate;
            if(depreciation.getToDate()==null&&depreciation.getExpDate().before(sDate))
                toDate = "Tài sản đã kết thúc khấu hao";
            else if(depreciation.getToDate()==null)
                toDate = "Đang sử dụng";
            else
                toDate = dateFormat.format(depreciation.getToDate());
            list.add(new DepreciationByAssetResponse.DepreciationAssetHistory(depreciation.getId()
                    ,userResponse
                    ,dateFormat.format(depreciation.getFromDate())
                    ,toDate
                    ,depreciation.getValueDepreciation()==null?value: depreciation.getValueDepreciation()
                    ,0
                    ,depreciationList));
        }
        //Tạo thông tin Response
        depreciationByAssetResponse.setValuePre(valuePre);
        depreciationByAssetResponse.setValuePrev(valuePrev);
        depreciationByAssetResponse.setLengthOfMonth(localDate.lengthOfMonth());
        depreciationByAssetResponse.setAmountDate(amountDate);
        depreciationByAssetResponse.setAmountMonth(assetResponse.getAmountOfYear());
        depreciationByAssetResponse.setAssetId(assetId);
        depreciationByAssetResponse.setAssetName(assetResponse.getAssetName());
        depreciationByAssetResponse.setPrice(assetResponse.getPrice());
        depreciationByAssetResponse.setFromDate(assetResponse.getDateUsed());
        depreciationByAssetResponse.setExpDate(assetResponse.getExpDate());
        depreciationByAssetResponse.setTotalValue(assetResponse.getPrice()-valuePre-valuePrev);
        depreciationByAssetResponse.setChangePrice("Không");
        depreciationByAssetResponse.setListDepreciationAssetHistory(list);
        return depreciationByAssetResponse;
    }
    public List<DepreciationHistoryByDepreciation> getDepreciationHistoryByDepreciation(Depreciation depreciation){
        List<DepreciationHistoryByDepreciation> list = new ArrayList<>();
        List<DepreciationHistory> depreciationHistories = depreciationHistoryService.findByDepreciation(depreciation);
        for(DepreciationHistory depreciationHistory : depreciationHistories){
            LocalDate localDate = LocalDate.of(depreciationHistory.getYear(),depreciationHistory.getMonth(),15);
            DepreciationHistoryByDepreciation depreciationHistoryByDepreciation  = list.stream()
                    .filter(o -> o.getYear() == depreciationHistory.getYear())
                    .findFirst()
                    .orElse(null);
            if(depreciationHistoryByDepreciation == null){
                depreciationHistoryByDepreciation = new DepreciationHistoryByDepreciation();
                depreciationHistoryByDepreciation.setYear(depreciationHistory.getYear());
                Map<String,Double> months = new HashMap<>();
                Map<String,String> dates = new HashMap<>();
                months.put(String.valueOf(depreciationHistory.getMonth()),depreciationHistory.getValue());
                Double result = (depreciationHistory.getValue()/depreciation.getValuePerMonth())*localDate.lengthOfMonth();
                dates.put(String.valueOf(depreciationHistory.getMonth()),Math.round(result)+"/"+localDate.lengthOfMonth());
                depreciationHistoryByDepreciation.setMonths(months);
                depreciationHistoryByDepreciation.setDates(dates);
                list.add(depreciationHistoryByDepreciation);
            }else{
                Map<String,Double> months = depreciationHistoryByDepreciation.getMonths();
                Map<String,String> dates = depreciationHistoryByDepreciation.getDates();
                months.put(String.valueOf(depreciationHistory.getMonth()),depreciationHistory.getValue());
                Double result = (depreciationHistory.getValue()/depreciation.getValuePerMonth())*localDate.lengthOfMonth();
                dates.put(String.valueOf(depreciationHistory.getMonth()),Math.round(result)+"/"+localDate.lengthOfMonth());
                depreciationHistoryByDepreciation.setMonths(months);
            }
        }
        return list;
    }

}
