package com.gh.mygreen.xlsmapper.fieldprocessor;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.*;
import static com.gh.mygreen.xlsmapper.TestUtils.*;
import static com.gh.mygreen.xlsmapper.xml.XmlBuilder.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.Map;

import com.gh.mygreen.xlsmapper.AnnotationInvalidException;
import com.gh.mygreen.xlsmapper.XlsMapper;
import com.gh.mygreen.xlsmapper.annotation.LabelledCellType;
import com.gh.mygreen.xlsmapper.annotation.XlsDateConverter;
import com.gh.mygreen.xlsmapper.annotation.XlsDefaultValue;
import com.gh.mygreen.xlsmapper.annotation.XlsLabelledCell;
import com.gh.mygreen.xlsmapper.annotation.XlsSheet;
import com.gh.mygreen.xlsmapper.annotation.XlsTrim;
import com.gh.mygreen.xlsmapper.util.CellPosition;
import com.gh.mygreen.xlsmapper.util.POIUtils;
import com.gh.mygreen.xlsmapper.validation.SheetBindingErrors;
import com.gh.mygreen.xlsmapper.xml.bind.XmlInfo;

/**
 * アノテーション{@link XlsDefaultValue}のテスタ。
 * <p>このテスタは、初期値の基本的な処理を確認するためのテスタです。
 *  <br>そのため、各クラスタイプの処理は、それぞれのコンバータのテスタで確認してください。
 * </p>
 *
 * @since 2.0
 * @author T.TSUCHIE
 *
 */
public class AnnoDefaultValueTest {
    
    /**
     * テスト結果ファイルの出力ディレクトリ
     */
    private static File OUT_DIR;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        OUT_DIR = createOutDir();
    }
    
    /**
     * 初期値の通常の読み込みのテスト 
     */
    @Test
    public void test_load_normal() throws Exception {
        
        XlsMapper mapper = new XlsMapper();
        mapper.getConiguration().setContinueTypeBindFailure(true);
        
        try(InputStream in = new FileInputStream("src/test/data/anno_DefaultValue.xlsx")) {
            SheetBindingErrors<NormalValueSheet> errors = mapper.loadDetail(in, NormalValueSheet.class);
            NormalValueSheet sheet = errors.getTarget();
            
            assertThat(sheet.existWithDefault).isEqualTo("あいう");
            assertThat(sheet.blank).isNull();
            assertThat(sheet.blankWithDefault).isEqualTo("  初期値  ");
            assertThat(sheet.blankWithDefaultTrim).isEqualTo("初期値");
            assertThat(sheet.blankWithDefaultFormat).isEqualTo(LocalDate.of(2017, 8, 20));
            
            
        }
        
    }
    
    /**
     * 初期値のフォーマットが不正なときのテスト
     */
    @Test
    public void test_load_invalidFormat() throws Exception {
        
        // アノテーションの組み立て
        XmlInfo xmlInfo = createXml()
                .classInfo(createClass(NormalValueSheet.class)
                        .field(createField("blankWithDefaultFormat")
                                .override(true)
                                .annotation(createAnnotation(XlsDefaultValue.class)
                                        .attribute("value", "abc")
                                        .buildAnnotation())
                                .buildField())
                        .buildClass())
                .buildXml();
        
        XlsMapper mapper = new XlsMapper();
        mapper.getConiguration().setContinueTypeBindFailure(true);
        mapper.getConiguration().setAnnotationMapping(xmlInfo);
        
        try(InputStream in = new FileInputStream("src/test/data/anno_DefaultValue.xlsx")) {
            
            assertThatThrownBy(() -> mapper.loadDetail(in, NormalValueSheet.class))
                .isInstanceOf(AnnotationInvalidException.class)
                .hasMessage("'com.gh.mygreen.xlsmapper.fieldprocessor.AnnoDefaultValueTest$NormalValueSheet#blankWithDefaultFormat' において、アノテーション @XlsDefaultValue の値 'abc' を 'java.time.LocalDate' に変換でませんでした。");
            
            
        }
        
    }
    
    /**
     * 初期値の通常の書き込み
     */
    @Test
    public void test_save_normal() throws Exception {
        
        // データ作成
        NormalValueSheet outSheet = new NormalValueSheet();
        outSheet.existWithDefault = "あいう";
        
        // ファイルへの書き込み
        XlsMapper mapper = new XlsMapper();
        mapper.getConiguration().setContinueTypeBindFailure(true);
        
        File outFile = new File(OUT_DIR, "anno_DefaultValue_out.xlsx");
        try(InputStream template = new FileInputStream("src/test/data/anno_DefaultValue_template.xlsx");
                OutputStream out = new FileOutputStream(outFile)) {
            
            mapper.save(template, out, outSheet);
        }
        
        // 書き込んだファイルを読み込み値の検証を行う。
        try(InputStream in = new FileInputStream(outFile)) {
            
            Sheet sheet = WorkbookFactory.create(in).getSheet("通常のテスト");
            {
                Cell cell = POIUtils.getCell(sheet, CellPosition.of("C4"));
                String text = POIUtils.getCellContents(cell, mapper.getConiguration().getCellFormatter());
                assertThat(text).isEqualTo("あいう");
            }
            
            {
                Cell cell = POIUtils.getCell(sheet, CellPosition.of("C5"));
                String text = POIUtils.getCellContents(cell, mapper.getConiguration().getCellFormatter());
                assertThat(text).isEqualTo("");
            }
            
            {
                Cell cell = POIUtils.getCell(sheet, CellPosition.of("C6"));
                String text = POIUtils.getCellContents(cell, mapper.getConiguration().getCellFormatter());
                assertThat(text).isEqualTo("  初期値  ");
            }
            
            {
                Cell cell = POIUtils.getCell(sheet, CellPosition.of("C7"));
                String text = POIUtils.getCellContents(cell, mapper.getConiguration().getCellFormatter());
                assertThat(text).isEqualTo("初期値");
            }
            
            {
                Cell cell = POIUtils.getCell(sheet, CellPosition.of("C8"));
                String text = POIUtils.getCellContents(cell, mapper.getConiguration().getCellFormatter());
                assertThat(text).isEqualTo("2017-08-20");
            }
            
        }
        
    }
    
    @XlsSheet(name="通常のテスト")
    private static class NormalValueSheet {
        
        private Map<String, Point> positions;
        
        @XlsDefaultValue(value="初期値")
        @XlsLabelledCell(label="値があるセル（初期値設定あり）", type=LabelledCellType.Right)
        private String existWithDefault;
        
        @XlsLabelledCell(label="空のセル", type=LabelledCellType.Right)
        private String blank;
        
        @XlsDefaultValue(value="  初期値  ")
        @XlsLabelledCell(label="空のセル（初期値設定あり）", type=LabelledCellType.Right)
        private String blankWithDefault;
        
        @XlsDefaultValue(value="  初期値  ")
        @XlsTrim
        @XlsLabelledCell(label="空のセル（初期値設定あり）(トリム指定)", type=LabelledCellType.Right)
        private String blankWithDefaultTrim;
        
        @XlsDefaultValue(value="2017-08-20")
        @XlsLabelledCell(label="空のセル（初期値設定あり）(書式指定あり)", type=LabelledCellType.Right)
        @XlsDateConverter(javaPattern="uuuu-MM-dd")
        private LocalDate blankWithDefaultFormat;
        
        
    } 

}
