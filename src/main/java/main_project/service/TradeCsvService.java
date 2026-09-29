package main_project.service;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;

import main_project.model.dto.TradeYearDto;

@Service
public class TradeCsvService {

    // CSV 파일들이 있는 폴더
    private final String folderPath ="C:/Users/Administrator/Desktop/정규 프로젝트/trade";

    public List<TradeYearDto> getYearComparison() {

        // ==============================
        // 1. 월별 합계 저장
        // ==============================

        Map<String, Double> monthlyBalance = new HashMap<>(); // 월별 무역수지 저장
        Map<String, Double> monthlyExport = new HashMap<>(); // 월별 수출액 합계 저장
        Map<String, Double> monthlyImport = new HashMap<>(); // 월별 수입액 합 저장


        File folder = new File(folderPath); // trade 폴더를 Java가 다룰 수 있게 객체로 만듦
        File[] files = folder.listFiles( // trade 폴더 안에 있는 파일들 중 이름이 .csv로 끝나는 파일만 전부 찾아라.
                (dir, name) ->name.toLowerCase().endsWith(".csv") // 파일 이름을 확인해서 .csv로 끝나면 선택한다. , toLowerCase : 대소문자가 달라도 전부 찾으려고 
        );


        if (files == null) { 
            System.out.println("CSV 폴더를 찾을 수 없습니다.");
            return new ArrayList<>();
        }


        // ==============================
        // 2. 모든 CSV 파일 읽기
        // ==============================

        for (File file : files) { //files 배열에 들어 있는 CSV 파일을 하나씩 꺼

            System.out.println("읽는 중 : " + file.getName());

            try (
                CSVReader reader = new CSVReader(new FileReader(file)) // 현재 CSV 파일을 읽을 수 있는 reader를 만든
            ) {

                String[] line; // CSV 한 줄을 담을 배열 변수를 만듦. ex) -23972,0,0,3506102000,23972,40,AD,안도라,플라스틱,2000.01 => line 에는 line[0] = "-23972" , line[1] = "0" , line[2] = "0"

                // 첫 줄 컬럼명 건너뛰기
                reader.readNext();


                while ((line = reader.readNext()) != null) { // CSV에서 다음 줄을 읽어서 line에 넣고, 읽을 줄이 있는 동안 계속 반복한다.
                    if (line.length < 10) {
                        continue;
                    }


                    // 필요한 컬럼만 꺼내기
                    double balPayments =
                            Double.parseDouble(
                                    line[0].trim() // trim = 공백 제거 ex) " 234 " -> "234"
                            );

                    double expDlr =
                            Double.parseDouble(
                                    line[1].trim()
                            );

                    double impDlr =
                            Double.parseDouble(
                                    line[4].trim()
                            );

                    String yearMonth =
                            line[9].trim();


                    // ==========================
                    // 월별 총합 누적
                    // ==========================

                    monthlyBalance.put( yearMonth, monthlyBalance.getOrDefault( yearMonth, 0.0 ) + balPayments ); // yearMonth라는 키가 이미 있으면 기존 값을 가져오고, 없으면 0.0을 가져와라
                    monthlyExport.put( yearMonth,monthlyExport.getOrDefault(yearMonth,0.0)+ expDlr);
                    monthlyImport.put(yearMonth,monthlyImport.getOrDefault(yearMonth,0.0)+ impDlr);}

            } catch (Exception e) {System.out.println(file.getName()+ " 읽는 중 오류 발생");
            e.printStackTrace();
            }
        }


        // ==============================
        // 3. 월별 합계를 연도별로 합치기
        // ==============================

        Map<Integer, Double> yearlyBalance = new HashMap<>(); // 연도별 무역수지 합계
        Map<Integer, Double> yearlyExport = new HashMap<>(); // 연도별 수출액 합계
        Map<Integer, Double> yearlyImport = new HashMap<>(); // 연도별 수입액 합

        Map<Integer, Integer> monthCount = new HashMap<>(); // 각 연도에 몇 개월 데이터가 있었는지 세는 Map ex) 2000년도에는 12개월치 / 2026년에는 9개월치
        // 키가 Integer인 이유가 월 단위 "2000.01"이 아니라 연도 2000만 저장하기 때문


        for (String yearMonth : monthlyExport.keySet()) {

            // "2000.01" → 2000
            int year = Integer.parseInt(yearMonth.substring(0, 4));


            yearlyBalance.put(year, yearlyBalance.getOrDefault(year, 0.0) + monthlyBalance.get(yearMonth)); // 그 해에 지금까지 더해놓은 무역수지 합계 + 이번 달 무역수지를 추가해서 다시 저장
            yearlyExport.put(year, yearlyExport.getOrDefault(year, 0.0) + monthlyExport.get(yearMonth));
            yearlyImport.put(year, yearlyImport.getOrDefault(year, 0.0) + monthlyImport.get(yearMonth));


            monthCount.put(year, monthCount.getOrDefault(year,0)+ 1);
        }


        // ==============================
        // 4. 연도별 월평균 계산
        // ==============================

        Map<Integer, Double> averageBalance = new HashMap<>();  
        Map<Integer, Double> averageExport = new HashMap<>();  
        Map<Integer, Double> averageImport = new HashMap<>();  

 
        for (Integer year : yearlyExport.keySet()) {

            int months = monthCount.get(year);

            averageBalance.put(
                    year,
                    yearlyBalance.get(year) / months
            );

            averageExport.put(
                    year,
                    yearlyExport.get(year) / months
            );

            averageImport.put(
                    year,
                    yearlyImport.get(year) / months
            );
        }


        // ==============================
        // 5. 연도 정렬
        // ==============================

        List<Integer> years =
                new ArrayList<>(
                        averageExport.keySet()
                );

        years.sort(Integer::compareTo);


        // ==============================
        // 6. 전년도 대비 증감률 계산
        // ==============================

        List<TradeYearDto> result =
                new ArrayList<>();


        for (Integer year : years) {

            double currentBalance =
                    averageBalance.get(year);

            double currentExport =
                    averageExport.get(year);

            double currentImport =
                    averageImport.get(year);


            Double balanceRate = null;
            Double exportRate = null;
            Double importRate = null;


            if (averageExport.containsKey(year - 1)) {

                double previousBalance =
                        averageBalance.get(year - 1);

                double previousExport =
                        averageExport.get(year - 1);

                double previousImport =
                        averageImport.get(year - 1);


                balanceRate =
                        calculateChangeRate(
                                previousBalance,
                                currentBalance
                        );


                exportRate =
                        calculateChangeRate(
                                previousExport,
                                currentExport
                        );


                importRate =
                        calculateChangeRate(
                                previousImport,
                                currentImport
                        );
            }


            TradeYearDto dto =
                    TradeYearDto.builder()
                            .year(year)
                            .balPayments(currentBalance)
                            .expDlr(currentExport)
                            .impDlr(currentImport)
                            .balPaymentsRate(balanceRate)
                            .expDlrRate(exportRate)
                            .impDlrRate(importRate)
                            .build();


            result.add(dto);
        }


        return result;
    }


    // ==============================
    // 증감률 계산
    // ==============================

    private Double calculateChangeRate(
            double previous,
            double current
    ) {

        if (previous == 0) {
            return null;
        }

        return (
                (current - previous)
                /
                Math.abs(previous)
        ) * 100;
    }
}