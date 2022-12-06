package com.example.team10searchengine.configuration.batch;

import com.example.team10searchengine.kordict.entity.KorDict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileItemReaderJobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final CsvReader csvReader;

    private final CsvWriter csvWriter;

    private static final int chunkSize = 5000;

    @Bean
    public Job csvFileItemReaderJob() {
        return jobBuilderFactory.get("csvFileItemReaderJob")
                .start(csvFileItemReaderStep())
                .build();
    }
    @Bean
    public Step csvFileItemReaderStep() {
        return stepBuilderFactory.get("csvFileItemReaderStep")
                .<KorDict, KorDict>chunk(chunkSize)
                .reader(csvReader.csvFileItemReader())
                .writer(csvWriter)
                .build();
    }
}