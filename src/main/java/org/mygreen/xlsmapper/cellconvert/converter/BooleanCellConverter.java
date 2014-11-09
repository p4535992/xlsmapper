package org.mygreen.xlsmapper.cellconvert.converter;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.mygreen.xlsmapper.POIUtils;
import org.mygreen.xlsmapper.Utils;
import org.mygreen.xlsmapper.XlsMapperConfig;
import org.mygreen.xlsmapper.annotation.converter.XlsBooleanConverter;
import org.mygreen.xlsmapper.annotation.converter.XlsConverter;
import org.mygreen.xlsmapper.cellconvert.AbstractCellConverter;
import org.mygreen.xlsmapper.cellconvert.TypeBindException;
import org.mygreen.xlsmapper.fieldprocessor.FieldAdaptor;


/**
 * Boolean/boolean型を処理するConverter.
 *
 * @author T.TSUCHIE
 *
 */
public class BooleanCellConverter extends AbstractCellConverter<Boolean> {
    
    @Override
    public Boolean toObject(final Cell cell, final FieldAdaptor adaptor, final XlsMapperConfig config) throws TypeBindException {
        
        final XlsConverter converterAnno = adaptor.getLoadingAnnotation(XlsConverter.class);
        final XlsBooleanConverter anno = getLoadingAnnotation(adaptor);
        
        if(cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
            return cell.getBooleanCellValue();
            
        } else {
            String cellValue = POIUtils.getCellContents(cell, config.getCellFormatter());
            cellValue = Utils.trim(cellValue, converterAnno);
            cellValue = Utils.getDefaultValueIfEmpty(cellValue, converterAnno);
            
            final Boolean result = convertFromString(cellValue, anno);
            if(result == null && Utils.isNotEmpty(cellValue)) {
                // 値が入っていて変換できない場合
                final String candidateValues = Utils.join(getLoadingAvailableValue(anno), ", ");
                
                throw newTypeBindException(cell, adaptor, cellValue)
                    .addMessageVar("candidateValues", candidateValues);
            }
            
            if(result != null) {
                return result;
            }
        }
        
        if(anno.failToFalse()) {
            return Boolean.FALSE;
        }
        
        if(adaptor.getTargetClass().isPrimitive()) {
            return Boolean.FALSE;
        }
        
        return null;
    }
    
    private XlsBooleanConverter getDefaultBooleanConverterAnnotation() {
        return new XlsBooleanConverter() {
            
            @Override
            public Class<? extends Annotation> annotationType() {
                return XlsBooleanConverter.class;
            }
            
            @Override
            public String saveAsTrue() {
                return "true";
            }
            
            @Override
            public String saveAsFalse() {
                return "false";
            }
            
            @Override
            public boolean ignoreCase() {
                return true;
            }
            
            @Override
            public String[] loadForTrue() {
                return new String[]{"true", "1", "yes", "on", "y", "t"};
            }
            
            @Override
            public String[] loadForFalse() {
                return new String[]{"false", "0", "no", "off", "f", "n"};
            }
            
            @Override
            public boolean failToFalse() {
                return false;
            }
        };
    }
    
    private XlsBooleanConverter getLoadingAnnotation(final FieldAdaptor adaptor) {
        XlsBooleanConverter anno = adaptor.getLoadingAnnotation(XlsBooleanConverter.class);
        if(anno == null) {
            anno = getDefaultBooleanConverterAnnotation();
        }
        
        return anno;
    }
    
    private XlsBooleanConverter getSavingAnnotation(final FieldAdaptor adaptor) {
        XlsBooleanConverter anno = adaptor.getSavingAnnotation(XlsBooleanConverter.class);
        if(anno == null) {
            anno = getDefaultBooleanConverterAnnotation();
        }
        
        return anno;
    }
    
    /**
     * 読み込み時の入力の候補となる値を取得する
     * @param anno
     * @return
     */
    private Collection<String> getLoadingAvailableValue(final XlsBooleanConverter anno) {
        
        final Set<String> values = new LinkedHashSet<String>();
        values.addAll(Arrays.asList(anno.loadForTrue()));
        values.addAll(Arrays.asList(anno.loadForFalse()));
        return values;
        
    }
    
    private Boolean convertFromString(final String value, final XlsBooleanConverter anno) {
        for(String trueValues : anno.loadForTrue()) {
            if(anno.ignoreCase() && value.equalsIgnoreCase(trueValues)) {
                return Boolean.TRUE;
                
            } else if(!anno.ignoreCase() && value.equals(trueValues)) {
                return Boolean.TRUE;
            }
        }
        
        for(String falseValues : anno.loadForFalse()) {
            if(anno.ignoreCase() && value.equalsIgnoreCase(falseValues)) {
                return Boolean.FALSE;
                
            } else if(!anno.ignoreCase() && value.equals(falseValues)) {
                return Boolean.FALSE;
            }
        }
        
        // 変換できない場合に強制的にエラーとする場合
        if(anno.failToFalse()) {
            return Boolean.FALSE;
        }
        
        return null;
    }
    
    @Override
    public Cell toCell(final FieldAdaptor adaptor, final Object targetObj, final Sheet sheet, final int column, final int row,
            final XlsMapperConfig config) {
        
        return toCell(adaptor, targetObj, sheet, column, row, config, null);
    }
    
    @Override
    public Cell toCellWithMap(final FieldAdaptor adaptor, final String key, final Object targetObj, final Sheet sheet, final int column, final int row,
            final XlsMapperConfig config) {
        
        return toCell(adaptor, targetObj, sheet, column, row, config, key);
    }
    
    private Cell toCell(final FieldAdaptor adaptor, final Object targetObj, final Sheet sheet, final int column, final int row,
            final XlsMapperConfig config, final String mapKey) {
        
        final XlsConverter converterAnno = adaptor.getSavingAnnotation(XlsConverter.class);
        final XlsBooleanConverter anno = getSavingAnnotation(adaptor);
        
        final Cell cell = POIUtils.getCell(sheet, column, row);
        
        // セルの書式設定
        if(converterAnno != null) {
            POIUtils.wrapCellText(cell, converterAnno.forceWrapText());
            POIUtils.shrinkToFit(cell, converterAnno.forceShrinkToFit());
        }
        
        Boolean value;
        if(mapKey == null) { 
            value = (Boolean) adaptor.getValue(targetObj);
        } else {
            value = (Boolean) adaptor.getValueOfMap(mapKey, targetObj);
        }
        
        // デフォルト値から値を設定する
        if(value == null && Utils.hasDefaultValue(converterAnno)) {
            value = convertFromString(Utils.getDefaultValue(converterAnno), anno);
        }
        
        if(value != null) {
            if(anno.saveAsTrue().equalsIgnoreCase("true") 
                    && anno.saveAsTrue().equalsIgnoreCase("false")
                    && cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
                // テンプレートのセルの書式がbooleanの場合はそのまま設定する
                cell.setCellValue(value);
            } else if(value) {
                cell.setCellValue(anno.saveAsTrue());
            } else {
                cell.setCellValue(anno.saveAsFalse());
            }
            
        } else {
            cell.setCellType(Cell.CELL_TYPE_BLANK);
        }
        
        return cell;
    }

}
