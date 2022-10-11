package com.example.team10searchengine;

import com.example.team10searchengine.entity.jpqlrepo.KorDictRepository;
import com.example.team10searchengine.entity.kordict.KorDict;
import com.example.team10searchengine.entity.kordict.dto.KorDictResponseDto;
import com.example.team10searchengine.entity.kordict.mybatisrepo.KorDictMapper;
import com.example.team10searchengine.shared.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
@Slf4j
@RequiredArgsConstructor
@Service
public class service {
    private final KorDictRepository korDictRepository;

    private final KorDictMapper korDictMapper;

    private List<Long> deleteId = new ArrayList<>();

    // 전체검색 paging
    @Transactional
    public Page<KorDict> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return korDictRepository.findAll(pageable);
    }

    // 1단어 입력 시 Btree검색 및 1단어 이외에는 fulltext검색
    @Transactional
    public List<KorDict> getKeyword(String keyword) {
        long startTime = System.currentTimeMillis();
        if(keyword.length() == 1)
             return korDictRepository.findByBtreeWord(keyword);

        if(keyword.contains(" "))
            keyword = keyword.replace(" ", "*+");

        List<KorDict> korDictList = korDictRepository.findByWord(keyword);

        long endTime = System.currentTimeMillis();
        log.info("time : {}" , endTime - startTime);
        return korDictList;
    }

    // 2단어씩 끊어서 like 검색 (Btree)
    @Transactional
    public List<KorDict>searchDictPropose(String keyword) {
        Long init = System.currentTimeMillis();
        String init_keyword = keyword;
        if(keyword.contains(" ")){
            keyword = keyword.replace(" ","%") + "%";
        }else{
            StringBuilder buf = new StringBuilder(keyword);
            int length = keyword.length();
            if(length <= 2){
                keyword = keyword + "%";
            }else{
                int offset = 2;
                int curlength = 2;
                while(curlength < length){
                    buf.insert(offset,"%");
                    offset += 3;
                    curlength += 2;
                }
                keyword = buf + "%";

            }
        }
        List<KorDict> korDictList = korDictRepository.findByBtreeWord(keyword);

        if(korDictList.isEmpty()){
            return korDictRepository.findByWord(init_keyword);
        }
        log.info(String.valueOf(System.currentTimeMillis() - init));
        log.info(keyword);
        return korDictList;
    }


    // Mybatis 테스트 (Btree)
    public List<KorDict> findByKeyword() {
        Long init = System.currentTimeMillis();
        List<KorDict> KorDictList = korDictMapper.findByKeyword();
        log.info("Time : {}",System.currentTimeMillis() - init);
        return KorDictList;

    }

    // Mybatis 테스트 (fulltext)
    public List<KorDict> findByNgramParser(String keyword) {
        Long init = System.currentTimeMillis();
        List<KorDict> KorDictList = korDictMapper.findByNgramParser(keyword);
        log.info("Time : {}",System.currentTimeMillis() - init);
        return KorDictList;
    }


    // Nooffset을 이용한 페이징 검색 (Mybatis)
    public ResponseEntity<?> findByNgramParserNoOffset(String keyword,Long korDictId,Long[] checkId) {
        Long init = System.currentTimeMillis();

        List<KorDict> KorDictList = korDictMapper.findByNgramParserNoOffset(keyword,korDictId);
        List<KorDictResponseDto> korDicts = new ArrayList<>();

        for(KorDict korDict : KorDictList) {
            korDicts.add(KorDictResponseDto.builder()
                    .id(korDict.getId())
                    .word(korDict.getWord())
                    .is_unique(korDict.getIsUnique())
                    .pronunciation(korDict.getPronunciation())
                    .part(korDict.getPart())
                    .meaning(korDict.getMeaning())
                    .example(korDict.getExample())
                    .field(korDict.getField())
                    .proverb(korDict.getProverb())
                    .idiom(korDict.getIdiom())
                    .classification(korDict.getClassification())
                    .build());
        }

        if(korDictId != null){
            for(int i=0; i<korDicts.size();i++) {
                for(Long delete : deleteId){
                    if(delete.equals(korDicts.get(i).getId())) {
                        korDicts.remove(i);
                    }
                }
                for(Long check : checkId){
                    if(check.equals(korDicts.get(i).getId())){
                        korDicts.remove(i);
                        deleteId.add(check);
                    }
                }
            }
        }

        log.info("deleteId : {}",deleteId);

        log.info("Time : {}",System.currentTimeMillis() - init);
        return new ResponseEntity<>(ResponseDto.success(korDicts), HttpStatus.OK);
    }

    // Nooffset을 이용한 페이징 검색 (Querydsl)
    @Transactional
    public List<KorDict> getKeywordNoOffset(String keyword, Long korDictId, Pageable pageable) {
        long startTime = System.currentTimeMillis();



        List<KorDict> korDictList = korDictRepository.findByWordUsingQuerydsl(keyword,korDictId,pageable);


        long endTime = System.currentTimeMillis();
        log.info("time : {}" , endTime - startTime);
        return korDictList;
    }

    @Scheduled(cron = "0 0 4 * * * ", zone = "Asia/Seoul")
    public void refreshDeleteId() {
        log.info("deleteId scheduler 작동");
        deleteId = new ArrayList<>();
    }

}
