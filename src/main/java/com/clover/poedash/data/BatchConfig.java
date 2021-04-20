package com.clover.poedash.data;

import javax.sql.DataSource;

import com.clover.poedash.model.Match;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@EnableBatchProcessing

public class BatchConfig {
    private final String CSV_PATH="hcssf-ladder-all.csv";
    private final String[] FIELD_NAMES = new String [] {
        "Rank", "Account", "Character", "CharacterClass", "Level", "Experience", "Dead"
    };

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public FlatFileItemReader<MatchInput> reader() {
        return new FlatFileItemReaderBuilder<MatchInput>().name("MatchItemReader")
        .resource(new ClassPathResource(CSV_PATH)).delimited()
        .names(FIELD_NAMES)
        .fieldSetMapper(new BeanWrapperFieldSetMapper<MatchInput>(){
            {
                setTargetType(MatchInput.class);
            }
        }).linesToSkip(1)
        .build();
        
    }

    @Bean
    public MatchDataProcessor processor(){
        return new MatchDataProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Match> writer(DataSource dataSource){
        return new JdbcBatchItemWriterBuilder<Match>()
        .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
        .sql("INSERT INTO match (id, account, character, character_class, level, experience, dead)"
        + " VALUES (:id, :account, :character, :characterClass, :level, :experience, :dead)")
        .dataSource(dataSource)
        .build();
    }

    @Bean
    public Job importUserJob(JobCompletionNotificationListener listener, Step step1){
        return jobBuilderFactory
        .get("importUserJob")
        .incrementer(new RunIdIncrementer())
        .listener(listener)
        .flow(step1)
        .end()
        .build();
    }

    @Bean
    public Step step1(JdbcBatchItemWriter<Match> writer){
        return stepBuilderFactory.get("step1").<MatchInput, Match>chunk(10)
        .reader(reader())
        .processor(processor())
        .writer(writer)
        .build();
    }

}
