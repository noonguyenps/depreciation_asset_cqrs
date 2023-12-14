package com.example.service.Impl;

import com.example.dto.response.DepreciationEvent;
import com.example.model.Depreciation;
import com.example.repository.DepreciationRepository;
import com.example.service.DepreciationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class DepreciationServiceImpl implements DepreciationService {
    private final DepreciationRepository depreciationRepository;
    @Override
    public List<Depreciation> findByAssetIdOrderByIdAsc(Long assetId) {
        return depreciationRepository.findByAssetIdOrderByIdAsc(assetId);
    }
    @Override
    public List<Depreciation> getDepreciationByFromDateAndToDate(Date fromDate, Date toDate) {
        return depreciationRepository.getDepreciationByFromDateAndToDate(fromDate,toDate);
    }

    @KafkaListener(topics = "depreciation-event-topic",groupId = "depreciation-event-group")
    public void processDepreciationEvents(DepreciationEvent depreciationEvent) {
        if(depreciationEvent.getEventType().equals("CreateDepreciation"))
            depreciationRepository.save(depreciationEvent.getDepreciation());
        else if(depreciationEvent.getEventType().equals("UpdateDepreciation")){
            Optional<Depreciation> depreciation = depreciationRepository.findById(depreciationEvent.getDepreciation().getId());
            if(depreciation.isPresent()){
                Depreciation depreciationTemp = depreciation.get();
                depreciationTemp.setAmountMonth(depreciationTemp.getAmountMonth());
                depreciationTemp.setValueDepreciation(depreciationTemp.getValueDepreciation());
                depreciationTemp.setToDate(depreciationTemp.getToDate());
                depreciationRepository.save(depreciationTemp);
            }
        }
    }


}
