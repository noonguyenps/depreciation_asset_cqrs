package com.example.mapping;

import com.example.client.DepreciationServiceClient;
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
public class DepreciationHistoryMapping {
    private final DepreciationServiceClient depreciationServiceClient;
    private final DepreciationHistoryService depreciationHistoryService;

    public DepreciationHistory getHistory(Depreciation depreciation) throws ParseException {
        Date fromDate = new Date();
        fromDate.setDate(1);
        DepreciationHistory depreciationHistory = new DepreciationHistory();
        depreciationHistory.setCreateAt(new Date());
        depreciationHistory.setMonth(fromDate.getMonth()+1);
        depreciationHistory.setYear(fromDate.getYear()+1900);
        depreciationHistory.setDepreciation(depreciation);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if(depreciation.getFromDate().before(fromDate))
            depreciationHistory.setValue(depreciationServiceClient.getDepreciationValue(depreciation.getAssetId(), dateFormat.format(fromDate),dateFormat.format(new Date())));
        else
            depreciationHistory.setValue(depreciationServiceClient.getDepreciationValue(depreciation.getAssetId(), dateFormat.format(depreciation.getFromDate()),dateFormat.format(new Date())));
        return depreciationHistory;
    }

    public List<AssetDepreciationResponse> entityToResponse(int month, int year){
        List<AssetDepreciationResponse> assetDepreciationResponseList = new ArrayList<>();
        for(Object a: depreciationHistoryService.getDepreciationValue(month, year)){
            Long assetId = Long.valueOf(((Object[])a)[0].toString());
            AssetResponse assetResponse = depreciationServiceClient.fetchAsset(assetId);
            AssetDepreciationResponse assetDepreciationResponse = new AssetDepreciationResponse();
            assetDepreciationResponse.setAssetId(assetId);
            assetDepreciationResponse.setSerialNumber(assetResponse.getSerial());
            assetDepreciationResponse.setPrice(assetResponse.getPrice());
            assetDepreciationResponse.setFromDate(assetResponse.getDateUsed());
            assetDepreciationResponse.setValuePerMonth(assetResponse.getPrice()/assetResponse.getAmountOfYear());
            assetDepreciationResponse.setAmountMonth(assetResponse.getAmountOfYear());
            Double accumulated = Double.valueOf(((Object[])a)[1].toString());
            assetDepreciationResponse.setAccumulated(accumulated);
            assetDepreciationResponse.setValuePresent(assetResponse.getPrice()-accumulated);
            assetDepreciationResponse.setAmountDayOfMonth(LocalDate.of(year,month,1).lengthOfMonth());
            assetDepreciationResponse.setAmountDateDepreciation(LocalDate.of(year,month,1).lengthOfMonth());
            LocalDate localDate = LocalDate.of(year,month,1).minusDays(1);
            Object accumulatedPrev = depreciationHistoryService.getValueByMonthAndYearAndAsset(localDate.getMonthValue(), localDate.getYear(), assetId);
            assetDepreciationResponse.setAccumulatedPrev(Double.valueOf(accumulatedPrev != null ? ((Object[])accumulatedPrev)[1].toString() : "0"));
            Object accumulatedPresent = depreciationHistoryService.getValueByMonthAndYearAndAsset(month, year,assetId);
            assetDepreciationResponse.setAccumulatedPresent(Double.valueOf((accumulatedPresent != null ? ((Object[])accumulatedPresent)[1].toString() :"0")));
            Map<String,Object> months = new HashMap<>();
            for(Object b: depreciationHistoryService.getValueByYear(year,assetId))
                months.put(((Object[])b)[0].toString(),Double.valueOf(((Object[])b)[1].toString()));
            assetDepreciationResponse.setMonths(months);
            Object o = depreciationHistoryService.getValueByMonthAndYearAndAsset(1, year,assetId);
            Double valueYearPrev = 0.0;
            if(o != null)
                valueYearPrev = Double.valueOf(((Object[])o)[1].toString());
            assetDepreciationResponse.setAccumulatedPresentPrev(assetResponse.getPrice()-valueYearPrev);
            assetDepreciationResponse.setAccumulatedYearPrev(valueYearPrev);
            assetDepreciationResponse.setAssetName(assetResponse.getAssetName());
            assetDepreciationResponseList.add(assetDepreciationResponse);
        }
        return assetDepreciationResponseList;

    }

//    public AssetDepreciationResponse getEntityToResponse(Long assetId){
//        Date date = new Date();
//        AssetDepreciationResponse assetDepreciationResponse = new AssetDepreciationResponse();
//        AssetResponse assetResponse = depreciationServiceClient.fetchAsset(assetId);
//        assetDepreciationResponse.setAssetId(assetId);
//        assetDepreciationResponse.setSerialNumber(assetResponse.getSerial());
//        assetDepreciationResponse.setPrice(assetResponse.getPrice());
//        assetDepreciationResponse.setFromDate(assetResponse.getDateUsed());
//        assetDepreciationResponse.setValuePerMonth(assetResponse.getPrice()/assetResponse.getAmountOfYear());
//        assetDepreciationResponse.setAmountMonth(assetResponse.getAmountOfYear());
//        // Giá trị khấu hao kì này
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        Double depreciationPresent = depreciationServiceClient.getDepreciationValue(assetId,(date.getYear()+1900)+"-"+(date.getMonth()+1)+"-01",dateFormat.format(date));
//        // Giá trị đã khấu hao
//        Object accumulated = depreciationHistoryService.getValueHistoryByAsset(date.getMonth()+1, date.getYear()+1900, assetId);
//        if(accumulated!= null){
//            assetDepreciationResponse.setAccumulated(Double.valueOf(((Object[])accumulated)[1].toString()));
//            assetDepreciationResponse.setValuePresent(assetResponse.getPrice()-Double.valueOf(((Object[])accumulated)[1].toString()));
//        }
//        else {
//            assetDepreciationResponse.setAccumulated(0.0);
//            assetDepreciationResponse.setValuePresent(assetResponse.getPrice());
//        }
//        assetDepreciationResponse.setAmountDayOfMonth(LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse((date.getYear()+1900)+"-"+(date.getMonth()+1)+"-01")).lengthOfMonth());
//        assetDepreciationResponse.setAmountDateDepreciation(date.getDate());
//        Map<String,Object> months = new HashMap<>();
//        assetDepreciationResponse.setAssetName(assetResponse.getAssetName());
//        return assetDepreciationResponse;
//    }
    //Tất cả thông tin khấu hao theo phòng ban
//    public List<DepreciationDeptResponse> getDepreciationDeptResponse(List<Object> data){
//        List<DepreciationDeptResponse> depreciationDeptResponses = new ArrayList<>();
//        for(Object o : data){
//            DepartmentResponse departmentResponse = depreciationServiceClient.fetchDepartment(Long.valueOf(((Object[])o)[0].toString()));
//            DepreciationDeptResponse depreciationDeptResponse = depreciationDeptResponses.stream()
//                    .filter(dept -> dept.getDeptId() == departmentResponse.getId())
//                    .findFirst()
//                    .orElse(null);
//            AssetResponse assetResponse = depreciationServiceClient.fetchAsset(Long.valueOf(((Object[])o)[1].toString()));
//            //Nếu chưa tồn tại dept thì thêm
//            if(depreciationDeptResponse == null){
//                List<AssetType> assetTypes =  new ArrayList<>();
//                depreciationDeptResponse = new DepreciationDeptResponse();
//                depreciationDeptResponse.setDeptName(departmentResponse.getName());
//                depreciationDeptResponse.setDeptId(departmentResponse.getId());
//                AssetType assetType = new AssetType();
//                assetType.setTypeId(assetResponse.getAssetTypeId());
//                assetType.setPrice(assetResponse.getPrice());
//                assetType.setTypeName(assetResponse.getAssetTypeName());
//                assetType.setValuePresent(Double.valueOf(((Object[])o)[2].toString()));
//                assetType.setValuePrev(Double.valueOf(((Object[])o)[3].toString()));
//                //Cần fix lại
//                assetType.setValuePerMonth(assetType.getValuePresent());
//                assetTypes.add(assetType);
//                depreciationDeptResponse.setAssetTypes(assetTypes);
//                depreciationDeptResponse.setTotalPrice(assetResponse.getPrice());
//                depreciationDeptResponse.setTotalValuePerMonth(0.0);
//                depreciationDeptResponse.setTotalValuePresent(Double.valueOf(((Object[])o)[2].toString()));
//                depreciationDeptResponse.setTotalValuePrev(Double.valueOf(((Object[])o)[3].toString()));
//                depreciationDeptResponses.add(depreciationDeptResponse);
//            }
//            //Đã tồn tại dept => thực hiện gọp assetType
//            else {
//                List<AssetType> assetTypes = depreciationDeptResponse.getAssetTypes();
//                System.out.println(assetTypes);
//                AssetType assetType = assetTypes.stream()
//                        .filter(type -> type.getTypeId() == assetResponse.getAssetTypeId())
//                        .findFirst()
//                        .orElse(null);
//                //AssetType chưa tồn tại thực hiện thêm mới
//                if(assetType == null){
//                    assetType.setTypeId(assetResponse.getAssetTypeId());
//                    assetType.setPrice(assetResponse.getPrice());
//                    assetType.setTypeName(assetResponse.getAssetTypeName());
//                    assetType.setValuePresent(Double.valueOf(((Object[])o)[2].toString()));
//                    assetType.setValuePrev(Double.valueOf(((Object[])o)[3].toString()));
//                    //Cần fix lại
//                    assetType.setValuePerMonth(0.0);
//                    assetTypes.add(assetType);
//                }//Tính tổng lại assetType
//                else{
//                    assetType.setPrice(assetResponse.getPrice()+assetType.getPrice());
//                    assetType.setValuePresent(Double.valueOf(((Object[])o)[2].toString())+assetType.getValuePresent());
//                    assetType.setValuePrev(Double.valueOf(((Object[])o)[3].toString())+assetType.getValuePrev());
//                    assetType.setValuePerMonth(0.0);
//                }
//                depreciationDeptResponse.setTotalPrice(depreciationDeptResponse.getTotalPrice()+assetResponse.getPrice());
//                depreciationDeptResponse.setTotalValuePerMonth(depreciationDeptResponse.getTotalValuePerMonth()+0.0);
//                depreciationDeptResponse.setTotalValuePresent(depreciationDeptResponse.getTotalValuePresent()+Double.valueOf(((Object[])o)[2].toString()));
//                depreciationDeptResponse.setTotalValuePrev(depreciationDeptResponse.getTotalValuePrev()+Double.valueOf(((Object[])o)[3].toString()));
//                depreciationDeptResponse.setAssetTypes(assetTypes);
//            }
//        }
//
//        return depreciationDeptResponses;
//    }

    public DepreciationByAssetResponse getDepreciationHistoryByAsset(Long assetId){
        DepreciationByAssetResponse depreciationByAssetResponse = new DepreciationByAssetResponse();
        AssetResponse assetResponse = depreciationServiceClient.fetchAsset(assetId);
        depreciationByAssetResponse.setAssetId(assetResponse.getAssetId());
        depreciationByAssetResponse.setAssetName(assetResponse.getAssetName());
        depreciationByAssetResponse.setPrice(assetResponse.getPrice());
        depreciationByAssetResponse.setFromDate(assetResponse.getDateUsed());
        depreciationByAssetResponse.setAmountMonth(assetResponse.getAmountOfYear());
        depreciationByAssetResponse.setExpDate(assetResponse.getExpDate());
        return null;
//        depreciationByAssetResponse.setValuePre(depreciationHistoryService.getValueHistoryByAsset(LocalDate.now().getMonthValue(),LocalDate.now().getYear(), assetId));
    }
    public void addDepreciationHistory(Depreciation depreciation){
        //Kéo lịch sử khấu hao đến cuối tháng
        Date today = new Date();
        Date lDate =  depreciation.getFromDate();
        int dayInMonth = lDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate().lengthOfMonth();
        //Nếu đã hết khấu hao thì chỉ cho phép tính khấu hao đến ngày kết thúc
        if(today.after(depreciation.getExpDate()))
            today = depreciation.getExpDate();
        //Nếu thời gian nhận tài sản bị kéo sang các tháng sau thì thực hiện kéo lịch sử khấu hao
        if((today.getMonth()>lDate.getMonth()&&today.getYear()==lDate.getYear())||today.getYear()>lDate.getYear()){
            //Kéo đến cuối tháng khi thu hồi tài sản
            DepreciationHistory depreciationHistory = new DepreciationHistory();
            depreciationHistory.setCreateAt(new Date());
            depreciationHistory.setMonth(lDate.getMonth()+1);
            depreciationHistory.setYear(lDate.getYear()+1900);
            depreciationHistory.setDepreciation(depreciation);
            depreciationHistory.setAssetId(depreciation.getAssetId());
            depreciationHistory.setAssetTypeId(depreciation.getAssetTypeId());
            depreciationHistory.setValue((Double.valueOf(dayInMonth-lDate.getDate())/dayInMonth)*depreciation.getValuePerMonth());
            depreciationHistoryService.saveDepreciationHistory(depreciationHistory);
            //Nếu thời gian sử dụng bị kéo sau x tháng
            int month = depreciation.getFromDate().getMonth()+1;
            for (int i = depreciation.getFromDate().getYear(); i <=today.getYear(); i++){
                for(int j = month; j<12;j++){
                    if(j==today.getMonth()&&i==today.getYear())
                        break;
                    depreciationHistory = new DepreciationHistory();
                    depreciationHistory.setCreateAt(new Date());
                    depreciationHistory.setMonth(j+1);
                    depreciationHistory.setYear(i+1900);
                    depreciationHistory.setDepreciation(depreciation);
                    depreciationHistory.setAssetId(depreciation.getAssetId());
                    depreciationHistory.setAssetTypeId(depreciation.getAssetTypeId());
                    depreciationHistory.setValue(depreciation.getValuePerMonth());
                    depreciationHistoryService.saveDepreciationHistory(depreciationHistory);
                }
            }
        }
    }

    public List<DepreciationDeptResponse> getDepreciationDeptResponse(List<Object> data){
        List<DepreciationDeptResponse> depreciationDeptResponses = new ArrayList<>();
        for(Object o : data) {
            //Lấy các thông tin từ DB
            Long deptId = Long.valueOf(((Object[]) o)[0].toString());
            Long assetTypeId = Long.valueOf(((Object[]) o)[1].toString());
            int month = Integer.valueOf(((Object[]) o)[2].toString());
            int year = Integer.valueOf(((Object[]) o)[3].toString());
            Double value = Double.valueOf(((Object[]) o)[4].toString());
            //Lấy giá trị dựa vào deptId
            Double valuePrev = depreciationHistoryService.getTotalValueByDeptId(deptId, year);
            // Gọp thông tin phòng ban
            DepreciationDeptResponse depreciationDeptResponse = depreciationDeptResponses.stream()
                    .filter(dept -> dept.getDeptId() == deptId)
                    .findFirst()
                    .orElse(null);
            if (depreciationDeptResponse == null) {
                depreciationDeptResponse = new DepreciationDeptResponse();
                DepartmentResponse departmentResponse = depreciationServiceClient.fetchDepartment(deptId);
                depreciationDeptResponse.setDeptId(departmentResponse.getId());
                depreciationDeptResponse.setDeptName(departmentResponse.getName());
                depreciationDeptResponse.setDepreciationPrev(valuePrev);
                depreciationDeptResponse.getMonths().put(String.valueOf(month), value);
                depreciationDeptResponses.add(depreciationDeptResponse);
            }else{
                if(depreciationDeptResponse.getMonths().get(String.valueOf(month))==null)
                    depreciationDeptResponse.getMonths().put(String.valueOf(month), value);
                else
                    depreciationDeptResponse.getMonths().put(String.valueOf(month), value + depreciationDeptResponse.getMonths().get(String.valueOf(month)));
            }
            //Tính tổng các giá trị của dept
            if(month == 1 || month == 2 || month == 3 )
                depreciationDeptResponse.setTotal1(value+depreciationDeptResponse.getTotal1());
            else if(month == 4 || month == 5 || month == 6)
                depreciationDeptResponse.setTotal2(value+depreciationDeptResponse.getTotal2());
            else if(month == 7 || month == 8 || month == 9)
                depreciationDeptResponse.setTotal3(value+depreciationDeptResponse.getTotal3());
            else
                depreciationDeptResponse.setTotal4(value+depreciationDeptResponse.getTotal4());
            depreciationDeptResponse.setTotalPrice(value+depreciationDeptResponse.getTotalPrice());
            //Gọp thông tin Loại tài sản
            List<AssetType> assetTypes = depreciationDeptResponse.getAssetTypes();
            AssetType assetType = assetTypes.stream()
                        .filter(type -> type.getTypeId() == assetTypeId)
                        .findFirst()
                        .orElse(null);
            //AssetType chưa tồn tại thực hiện thêm mới
            if(assetType == null) {
                assetType = new AssetType();
                assetType.setTypeId(assetTypeId);
                Double valueTypePrev = depreciationHistoryService.getTotalValueByDeptIdAndAssetType(deptId,assetTypeId, year);
                AssetTypeResponse assetTypeResponse = depreciationServiceClient.fetchAssetType(assetTypeId);
                assetType.setTypeName(assetTypeResponse.getAssetName());
                assetType.setDepreciationPrev(valueTypePrev);
                assetType.setTotalPrice(value);
                assetType.getMonths().put(String.valueOf(month),value);
                assetTypes.add(assetType);
            }else {
                assetType.setTotalPrice(value+assetType.getTotalPrice());
                assetType.getMonths().put(String.valueOf(month),value);
            }
            //Tính tổng các giá trị của assetType
            if(month == 1 || month == 2 || month == 3 )
                assetType.setTotal1(value+assetType.getTotal1());
            else if(month == 4 || month == 5 || month == 6)
                assetType.setTotal2(value+assetType.getTotal2());
            else if(month == 7 || month == 8 || month == 9)
                assetType.setTotal3(value+assetType.getTotal3());
            else
                assetType.setTotal4(value+assetType.getTotal4());
        }
        return depreciationDeptResponses;
    }
}
