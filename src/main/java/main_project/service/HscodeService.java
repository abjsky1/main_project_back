package main_project.service;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import main_project.model.dto.HscodeDto;
import main_project.model.entity.HscodeEntity;
import main_project.model.repository.HscodeRepository;

@Service
@RequiredArgsConstructor
public class HscodeService {

    private final HscodeRepository hscodeRepository;


    // 1. Excel -> CSV 변환
    public boolean excelToCsv() {

        String excelFile =
            "src/main/resources/static/hscode/hscode.xlsx";

        String csvFile =
            "src/main/resources/static/hscode/hscode.csv";

        try (
            FileInputStream fis =
                new FileInputStream(excelFile);

            Workbook workbook =
                new XSSFWorkbook(fis);

            BufferedWriter bw =
                new BufferedWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(csvFile),
                        StandardCharsets.UTF_8
                    )
                )
        ) {

            Sheet sheet = workbook.getSheetAt(0);

            DataFormatter formatter =
                new DataFormatter();

            // UTF-8 BOM
            bw.write("\uFEFF");

            // CSV 헤더
            bw.write("hscode_id,hscode_name");
            bw.newLine();

            for (Row row : sheet) {
                String hscodeId = formatter
                        .formatCellValue(row.getCell(0))
                        .trim();

                String hscodeName = formatter
                        .formatCellValue(row.getCell(1))
                        .trim();

                if (hscodeId.isEmpty()) {continue;}

                HscodeDto dto = new HscodeDto(hscodeId,hscodeName);

                String name = dto.getHscode_name().replace("\"", "\"\"");

                bw.write(
                    "\"" + dto.getHscode_id()
                    + "\",\""
                    + name
                    + "\""
                );
                bw.newLine();
            }
            System.out.println("CSV 변환 완료!");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // 2. HS CODE 하나 DB 저장
    public boolean create(HscodeDto dto) {
        try {
                HscodeEntity entity =
                HscodeEntity.builder()
                    .hscodeId(dto.getHscode_id())
                    .hscodeName(dto.getHscode_name())
                    .build();

            hscodeRepository.save(entity);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // 3. DB 전체 조회
    public List<HscodeEntity> findAll() {return hscodeRepository.findAll();}


    // 4. DB 개별 조회
    public HscodeEntity findById(String hscodeId) {
            return hscodeRepository
            .findById(hscodeId)
            .orElse(null);
    }

}