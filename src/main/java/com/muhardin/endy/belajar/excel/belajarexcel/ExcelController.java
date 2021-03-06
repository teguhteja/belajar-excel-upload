package com.muhardin.endy.belajar.excel.belajarexcel;

import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller @Slf4j
@SessionAttributes("konfigurasiKelas")
public class ExcelController {

    private static final Integer ROW_START_HEADER = 0;
    private static final Integer ROW_START_DAFTAR_NILAI = ROW_START_HEADER + 5;

    @Value("classpath:logo.png")
    private Resource logo;

    private Faker faker = new Faker(new Locale("id", "id"));

    @GetMapping("/")
    public ModelAndView tampilkanFormKelas() {
        return new ModelAndView("kelas",
                new ModelMap().addAttribute("kelas", new KelasDto()));
    }

    @PostMapping("/")
    public String prosesFormKelas(@ModelAttribute("kelas") @Valid KelasDto kelas, BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            return "kelas";
        }
        model.addAttribute("konfigurasiKelas", kelas);
        return "redirect:/upload/form";
    }

    @GetMapping("/upload/form")
    public ModelMap displayFormUpload(@ModelAttribute("konfigurasiKelas") KelasDto kelas) {
        return new ModelMap().addAttribute("kelas", kelas);
    }

    @GetMapping("/template-nilai.xlsx")
    public void downloadTemplateNilai(@ModelAttribute("konfigurasiKelas") KelasDto kelas, HttpServletResponse response) throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Nilai UTS");

        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 15000);
        sheet.setColumnWidth(2, 5000);

        pasangLogo(wb, sheet);
        createHeader(sheet, kelas);

        List<MahasiswaDto> daftarMahasiswa = generateDaftarMahasiswa(kelas.getJumlahMahasiswa());
        createDaftarNilai(sheet, daftarMahasiswa);

        response.setHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=nilai-uts.xlsx");
        wb.write(response.getOutputStream());
    }

    private void pasangLogo(Workbook wb, Sheet sheet) throws IOException {
        sheet.addMergedRegion(new CellRangeAddress(
                0,
                2,
                0,
                0
        ));

        byte[] baLogo = IOUtils.toByteArray(logo.getInputStream());
        int pictureIdx = wb.addPicture(baLogo, Workbook.PICTURE_TYPE_PNG);
        logo.getInputStream().close();

        CreationHelper helper = wb.getCreationHelper();
        Drawing drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
        anchor.setCol1(0);
        anchor.setCol2(1);
        anchor.setRow1(0);
        anchor.setRow2(3);
        drawing.createPicture(anchor, pictureIdx);
    }

    private List<MahasiswaDto> generateDaftarMahasiswa(Integer jumlah) {
        List<MahasiswaDto> daftarMahasiswa = new ArrayList<>();
        for (int i = 0; i < jumlah; i++) {
            daftarMahasiswa.add(
                    new MahasiswaDto(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                            + String.format("%03d", i+1), faker.name().fullName()));
        }

        return  daftarMahasiswa;
    }

    private void createHeader(Sheet sheet, KelasDto kelas) {
        XSSFColor orens = new XSSFColor(new byte[]{(byte) 244, (byte) 121, (byte) 32}, null);

        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        font.setFontName("arial");
        XSSFCellStyle style = (XSSFCellStyle) sheet.getWorkbook().createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(orens);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        enableBorder(style);

        Row row1 = sheet.createRow(ROW_START_HEADER);
        row1.setHeightInPoints(30);
        row1.createCell(1).setCellValue("Kode Matakuliah");
        row1.createCell(2).setCellValue(kelas.getKodeMatakuliah());

        Row row2 = sheet.createRow(ROW_START_HEADER + 1);
        row2.setHeightInPoints(30);
        row2.createCell(1).setCellValue("Nama Matakuliah");
        row2.createCell(2).setCellValue(kelas.getNamaMatakuliah());

        Row row3 = sheet.createRow(ROW_START_HEADER + 2);
        row3.setHeightInPoints(30);
        row3.createCell(1).setCellValue("Nama Dosen");
        row3.createCell(2).setCellValue(kelas.getNamaDosen());

        row1.getCell(1).setCellStyle(style);
        row1.getCell(2).setCellStyle(style);
        row2.getCell(1).setCellStyle(style);
        row2.getCell(2).setCellStyle(style);
        row3.getCell(1).setCellStyle(style);
        row3.getCell(2).setCellStyle(style);

    }

    private void createDaftarNilai(Sheet sheet, List<MahasiswaDto> daftarMahasiswa) {

        XSSFColor biru = new XSSFColor(new byte[]{(byte) 2, (byte) 62, (byte) 136}, null);

        XSSFFont fontContent = (XSSFFont) sheet.getWorkbook().createFont();
        fontContent.setFontName("Arial");
        fontContent.setColor(biru);
        CellStyle styleContent = sheet.getWorkbook().createCellStyle();
        enableBorder(styleContent);
        styleContent.setFont(fontContent);

        Font fontHeader = sheet.getWorkbook().createFont();
        fontHeader.setFontName("Arial");
        fontHeader.setBold(true);
        fontHeader.setColor(IndexedColors.WHITE.getIndex());

        XSSFCellStyle styleHeader = (XSSFCellStyle) sheet.getWorkbook().createCellStyle();
        enableBorder(styleHeader);
        styleHeader.setFont(fontHeader);
        styleHeader.setFillForegroundColor(biru);
        styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // header
        Row hdr = sheet.createRow(ROW_START_DAFTAR_NILAI);
        hdr.createCell(0).setCellValue("NIM");
        hdr.createCell(1).setCellValue("Nama Mahasiswa");
        hdr.createCell(2).setCellValue("Nilai");

        hdr.getCell(0).setCellStyle(styleHeader);
        hdr.getCell(1).setCellStyle(styleHeader);
        hdr.getCell(2).setCellStyle(styleHeader);

        // data mahasiswa
        int baris = ROW_START_DAFTAR_NILAI;
        for (MahasiswaDto m : daftarMahasiswa) {
            baris++;

            Row mhs = sheet.createRow(baris);
            mhs.createCell(0).setCellValue(m.getNim());
            mhs.createCell(1).setCellValue(m.getNama());

            mhs.getCell(0).setCellStyle(styleContent);
            mhs.getCell(1).setCellStyle(styleContent);
            mhs.createCell(2).setCellStyle(styleContent);
        }
    }

    private void enableBorder(CellStyle cellStyle) {
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
    }

    @PostMapping("/upload/form")
    public String prosesFormUpload(
            @ModelAttribute("konfigurasiKelas") KelasDto kelas,
            @RequestParam("nilai") MultipartFile fileNilai,
            RedirectAttributes redirectAttrs, SessionStatus status) {

        log.debug("Nama file : {}", fileNilai.getOriginalFilename());
        log.debug("Ukuran file : {} bytes", fileNilai.getSize());

        List<NilaiDto> hasilPenilaian = new ArrayList<>();

        try {
            Workbook workbook = new XSSFWorkbook(fileNilai.getInputStream());
            Sheet sheetPertama = workbook.getSheetAt(0);
            int rowPertama = ROW_START_DAFTAR_NILAI + 1;
            int jumlahMahasiswa = kelas.getJumlahMahasiswa();
            for (int i = 0; i < jumlahMahasiswa; i++) {
                Row baris = sheetPertama.getRow(rowPertama + i);

                NilaiDto nilaiDto = new NilaiDto();

                Cell cellNim = baris.getCell(0); // tipe cell nim bisa string atau numeric, harus dihandle
                if (CellType.NUMERIC.equals(cellNim.getCellType())) {
                    nilaiDto.setNim(new BigDecimal(
                            cellNim.getNumericCellValue())
                            .setScale(0, RoundingMode.UNNECESSARY)
                            .toString());
                } else if (CellType.STRING.equals(cellNim.getCellType())) {
                    nilaiDto.setNim(cellNim.getStringCellValue());
                }

                nilaiDto.setNama(baris.getCell(1).getStringCellValue());
                nilaiDto.setNilai(new BigDecimal(baris.getCell(2).getNumericCellValue()));

                hasilPenilaian.add(nilaiDto);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        redirectAttrs.addFlashAttribute("hasilPenilaian", hasilPenilaian);
        status.setComplete();
        return "redirect:hasil";
    }

    @GetMapping("/upload/hasil")
    public ModelMap tampilkanHasilUpload(@ModelAttribute("hasilPenilaian") List<NilaiDto> daftarNilai) {
        return new ModelMap()
                .addAttribute("daftarNilai", daftarNilai);
    }
}
