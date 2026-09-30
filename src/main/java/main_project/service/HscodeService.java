package main_project.service;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import main_project.model.dto.HscodeDto;

@Service
public class HscodeService {

    // CSV 파일 경로
    private final String csvFile = "src/main/resources/static/hscode/hscode.csv";




    // HS CODE 전체 조회
    public List<HscodeDto> findAll() {
        List<HscodeDto> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Path.of(csvFile),StandardCharsets.UTF_8))
         {

            String line;

            // 첫 번째 줄 헤더 건너뛰기
            br.readLine();
            while ((line = br.readLine()) != null) {

                String[] data = line.split(",", 2);
                if (data.length < 2) {continue;}

                String hscodeId = data[0]
                        .replace("\"", "")
                        .replace("\uFEFF", "")
                        .trim();

                String hscodeName = data[1]
                        .replace("\"", "")
                        .trim();
                HscodeDto dto = new HscodeDto(
                        hscodeId,
                        hscodeName
                    );
                list.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    // HS CODE 단건 조회
    public HscodeDto findById(String hscodeId) {
        List<HscodeDto> list = findAll();
        for (HscodeDto dto : list) {
        if (dto.getHscode_id().equals(hscodeId)) {
                return dto;
            }
        }
        return null;
    }
}