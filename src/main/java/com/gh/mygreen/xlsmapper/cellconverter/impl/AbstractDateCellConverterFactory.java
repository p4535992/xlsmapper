package com.gh.mygreen.xlsmapper.cellconverter.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import com.gh.mygreen.xlsmapper.Configuration;
import com.gh.mygreen.xlsmapper.annotation.XlsDateConverter;
import com.gh.mygreen.xlsmapper.cellconverter.AbstractCellConverter;
import com.gh.mygreen.xlsmapper.cellconverter.AbstractCellConverterFactorySupport;
import com.gh.mygreen.xlsmapper.cellconverter.CellConverterFactory;
import com.gh.mygreen.xlsmapper.fieldaccessor.FieldAccessor;
import com.gh.mygreen.xlsmapper.textformatter.TextFormatter;
import com.gh.mygreen.xlsmapper.textformatter.TextParseException;
import com.gh.mygreen.xlsmapper.util.Utils;

/**
 * {@link Date}または、その子クラスに対する{@link CellConverterFactory}のベースクラス。
 * 
 * @since 2.0
 * @author T.TSUCHIE
 *
 */
public abstract class AbstractDateCellConverterFactory<T extends Date> extends AbstractCellConverterFactorySupport<T>
        implements CellConverterFactory<T> {
    
    @Override
    protected void setupCustom(final AbstractCellConverter<T> cellConverter, final FieldAccessor field, final Configuration config) {
        
        if(cellConverter instanceof AbstractDateCellConverter) {
            
            final AbstractDateCellConverter<T> dateCellConverter = (AbstractDateCellConverter<T>)cellConverter;
            
            // 書き込み時のセルの書式を設定する
            Optional<XlsDateConverter> converterAnno = field.getAnnotation(XlsDateConverter.class);
            String excelPattern = getExcelPattern(converterAnno);
            
            dateCellConverter.setExcelPattern(excelPattern);
            
        }
        
    }
    
    @Override
    protected TextFormatter<T> createTextFormatter(final FieldAccessor field, final Configuration config) {
        
        final Optional<XlsDateConverter> converterAnno = field.getAnnotation(XlsDateConverter.class);
        DateFormat formatter = createFormatter(converterAnno);
        
        return new TextFormatter<T>() {
            
            @Override
            public T parse(final String text) {
                try {
                    Date date = formatter.parse(text);
                    return convertTypeValue(date);
                    
                } catch(ParseException e) {
                    final Map<String, Object> vars = new HashMap<>();
                    vars.put("javaPattern", getJavaPattern(converterAnno));
                    vars.put("excelPattern", getExcelPattern(converterAnno));
                    
                    throw new TextParseException(text, field.getType(), e, vars);
                }
            }
            
            @Override
            public String format(final T value) {
                return formatter.format(value);
            }
        };
        
    }
    
    /**
     * アノテーションを元に日時のフォーマッタを作成する。
     * @param converterAnno アノテーション
     * @return 日時のフォーマッタ
     */
    protected DateFormat createFormatter(final Optional<XlsDateConverter> converterAnno) {
        
        final boolean lenient = converterAnno.map(a -> a.lenient()).orElse(false);
        if(!converterAnno.isPresent()) {
            SimpleDateFormat formatter = new SimpleDateFormat(getDefaultJavaPattern());
            formatter.setLenient(lenient);
            return formatter;
        }
        
        final String pattern = getJavaPattern(converterAnno);
        
        final Locale locale = Utils.getLocale(converterAnno.get().locale());
        final TimeZone timeZone = converterAnno.get().timezone().isEmpty() ? TimeZone.getDefault()
                : TimeZone.getTimeZone(converterAnno.get().timezone());
        
        final SimpleDateFormat formatter = new SimpleDateFormat(pattern, locale);
        formatter.setLenient(lenient);
        formatter.setTimeZone(timeZone);
        
        return formatter;
    }
    
    private String getJavaPattern(final Optional<XlsDateConverter> converterAnno) {
        if(!converterAnno.isPresent()) {
            return getDefaultJavaPattern();
        }
        
        String pattern = converterAnno.get().javaPattern();
        if(pattern.isEmpty()) {
            pattern = getDefaultJavaPattern();
        }
        
        return pattern;
    }
    
    private String getExcelPattern(final Optional<XlsDateConverter> converterAnno) {
        if(!converterAnno.isPresent()) {
            return getDefaultExcelPattern();
        }
        
        String pattern = converterAnno.get().excelPattern();
        if(pattern.isEmpty()) {
            pattern = getDefaultExcelPattern();
        }
        
        return pattern;
    }
    
    /**
     * その型における型に変換する
     * @param value 変換対象の値
     * @return 変換後の値
     */
    protected abstract T convertTypeValue(Date date);
    
    
    /**
     * その型における標準のJavaの書式を返す。
     * @since 0.5
     * @return {@link SimpleDateFormat}で処理可能な形式。
     */
    protected abstract String getDefaultJavaPattern();
    
    /**
     * その型における標準のExcelの書式を返す。
     * @since 1.1
     * @return Excelの書式
     */
    protected abstract String getDefaultExcelPattern();
    
    
}
