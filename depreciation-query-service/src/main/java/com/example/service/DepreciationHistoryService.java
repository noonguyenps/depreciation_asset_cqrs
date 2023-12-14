package com.example.service;

import com.example.model.Depreciation;
import com.example.model.DepreciationHistory;

import java.util.List;

public interface DepreciationHistoryService {

    boolean saveDepreciationHistory(DepreciationHistory depreciationHistory);
    List<DepreciationHistory> findByDepreciation(Depreciation depreciation);
    List<Object> getDepreciationValue(int month,int year);
    Object getValueByMonthAndYearAndAsset(int month,int year, Long assetId);
    List<Object> getValueByYear(int year, Long assetId);
    Object getValueHistoryByDepreciation(int month, int year, Long depreciationId);
    List<Object> getDepreciationByAllDept(int year);
    List<Object> getDepreciationByDeptIds(int year,List<Long> deptIds);
    Double totalValueDepreciation();
    Double getTotalValueByDeptIdAndAssetType(Long deptId, Long assetTypeId, int year);
    Double totalValueDepreciationByAssetId(Long assetId);
    Double totalValueDepreciationByAssetId(Long assetId, int month, int year);
    Double totalValueDepreciationByDepreciationId(Long depreciationId, int month, int year);
    Double getTotalValueByDeptId(Long deptId, int year);
}
