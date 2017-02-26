package com.gh.mygreen.xlsmapper.converter.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import com.gh.mygreen.xlsmapper.XlsMapperConfig;
import com.gh.mygreen.xlsmapper.XlsMapperException;
import com.gh.mygreen.xlsmapper.annotation.XlsCellOption;
import com.gh.mygreen.xlsmapper.annotation.XlsDateConverter;
import com.gh.mygreen.xlsmapper.annotation.XlsDefaultValue;
import com.gh.mygreen.xlsmapper.annotation.XlsFormula;
import com.gh.mygreen.xlsmapper.annotation.XlsTrim;
import com.gh.mygreen.xlsmapper.converter.AbstractCellConverter;
import com.gh.mygreen.xlsmapper.processor.FieldAdapter;
import com.gh.mygreen.xlsmapper.util.ConversionUtils;
import com.gh.mygreen.xlsmapper.util.POIUtils;
import com.gh.mygreen.xlsmapper.util.Utils;


/**
 * {@link Calendar}型の変換用クラス。
 *
 * @since 1.5
 * @author T.TSUCHIE
 *
 */
public class CalendarCellConverter extends AbstractCellConverter<Calendar> {
    
    private DateCellConverter dateConverter;
    
    public CalendarCellConverter() {
        this.dateConverter = new DateCellConverter();
    }
    
    @Override
    public Calendar toObject(final Cell cell, final FieldAdapter adapter, final XlsMapperConfig config)
            throws XlsMapperException {
        
        final Date date = dateConverter.toObject(cell, adapter, config);
        Calendar cal = null;
        if(date != null) {
            cal = Calendar.getInstance();
            cal.setTime(date);
        }
        
        return cal;
    }
    
    @Override
    public Cell toCell(final FieldAdapter adapter, final Calendar targetValue, final Object targetBean,
            final Sheet sheet, final int column, final int row,
            final XlsMapperConfig config) throws XlsMapperException {
        
        final Optional<XlsDefaultValue> defaultValueAnno = adapter.getAnnotation(XlsDefaultValue.class);
        final Optional<XlsTrim> trimAnno = adapter.getAnnotation(XlsTrim.class);
        
        final XlsDateConverter anno = adapter.getAnnotation(XlsDateConverter.class)
                .orElseGet(() ->dateConverter.getDefaultDateConverterAnnotation());
        
        final Optional<XlsFormula> formulaAnno = adapter.getAnnotation(XlsFormula.class);
        final boolean primaryFormula = formulaAnno.map(a -> a.primary()).orElse(false);
        
        final Cell cell = POIUtils.getCell(sheet, column, row);
        
        // セルの書式設定
        ConversionUtils.setupCellOption(cell, adapter.getAnnotation(XlsCellOption.class));
        
        Calendar value = targetValue;
        
        // デフォルト値から値を設定する
        if(value == null && defaultValueAnno.isPresent()) {
            final String defaultValue = defaultValueAnno.get().value();
            final DateFormat formatter;
            
            if(Utils.isNotEmpty(anno.javaPattern())) {
                formatter = dateConverter.createDateFormat(anno);
            } else {
                formatter = dateConverter.createDateFormat(dateConverter.getDefaultDateConverterAnnotation());
            }
            
            try {
                Date date = dateConverter.parseDate(defaultValue, formatter);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                value = cal;
            } catch (ParseException e) {
                throw newTypeBindException(e, cell, adapter, defaultValue)
                    .addAllMessageVars(dateConverter.createTypeErrorMessageVars(anno));
            }
            
        }
        
        // セルの書式の設定
        if(Utils.isNotEmpty(anno.javaPattern())) {
            cell.getCellStyle().setDataFormat(POIUtils.getDataFormatIndex(sheet, anno.javaPattern()));
        }
        
        if(value != null && !primaryFormula) {
            cell.setCellValue(value);
            
        } else if(formulaAnno.isPresent()) {
            Utils.setupCellFormula(adapter, formulaAnno.get(), config, cell, targetBean);
            
        } else {
            cell.setCellType(Cell.CELL_TYPE_BLANK);
        }
        
        return cell;
    }
    
    
}
