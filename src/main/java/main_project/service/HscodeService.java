package main_project.service;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import main_project.model.dto.HscodeDto;

@Service
public class HscodeService {

    // Excel -> CSV 변환
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

            // 한글 깨짐 방지용 BOM
            bw.write("\uFEFF");

            // CSV 헤더
            bw.write("hscode_id,hscode_name");
            bw.newLine();

            for (Row row : sheet) {

                String hscodeId =
                    formatter
                        .formatCellValue(row.getCell(0))
                        .trim();

                String hscodeName =
                    formatter
                        .formatCellValue(row.getCell(1))
                        .trim();

                // 빈 행이면 건너뜀
                if (hscodeId.isEmpty()) {
                    continue;
                }

                HscodeDto dto =
                    new HscodeDto(
                        hscodeId,
                        hscodeName
                    );

                // CSV 내부 큰따옴표 처리
                String name =
                    dto.getHscode_name()
                       .replace("\"", "\"\"");

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
}