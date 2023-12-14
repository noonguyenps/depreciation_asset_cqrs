package com.example.service.Impl;

import com.example.dto.response.DepreciationEvent;
import com.example.model.Depreciation;
import com.example.repository.DepreciationRepository;
import com.example.service.DepreciationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class DepreciationServiceImpl implements DepreciationService {
    @Autowired
    private KafkaTemplate<String,Object> kafkaTemplate;

    private final DepreciationRepository depreciationRepository;

    @Override
    public Depreciation findByAssetIdAndToDate(Long assetId) {
        Optional<Depreciation> depreciation = depreciationRepository.findByAssetIdAndToDate(assetId);
        if(depreciation.isEmpty())
            return null;
        return depreciation.get();
    }

    @Override
    public Depreciation createDepreciation(Depreciation depreciation ){
        Depreciation depreciationTemp = depreciationRepository.save(depreciation);
        DepreciationEvent event = new DepreciationEvent("CreateDepreciation",depreciationTemp);
        kafkaTemplate.send("depreciation-event-topic", event);
        return depreciationTemp;
    }

    @Override
    public Depreciation saveDepreciation(Depreciation depreciation ){
        Depreciation depreciationTemp = depreciationRepository.save(depreciation);
        DepreciationEvent event = new DepreciationEvent("UpdateDepreciation",depreciationTemp);
        kafkaTemplate.send("depreciation-event-topic", event);
        return depreciationTemp;
    }

    @Override
    public Depreciation findDepreciationToUpdate(Long assetId){
        Optional<Depreciation> depreciation = depreciationRepository.findDepreciationIsNull(assetId);
        if(depreciation.isPresent())
            return depreciation.get();
        return null;
    }


    @Override
    public List<Depreciation> getDepreciationByFromDateAndToDate(Date fromDate, Date toDate) {
        return depreciationRepository.getDepreciationByFromDateAndToDate(fromDate,toDate);
    }

    @Override
    public Object findLDateAndSumValueByAssetId(Long assetId) {
        return depreciationRepository.findLastDepreciationByAssetId(assetId);
    }
}
