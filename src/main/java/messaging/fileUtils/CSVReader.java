package messaging.fileUtils;

import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import util.BaseBean;

import java.io.*;
import java.util.List;
import java.util.UUID;

public  class CSVReader implements Reader {

    private RowValidator rowValidator;

    @Override
    public boolean readFile(InputStream inputStream, String[] inputColumns, String[] inputHeaders, BaseBean fileBean, List<BaseBean> tables) {
        String id = UUID.randomUUID().toString();
//        fileBean.setString("document-id", id);
        java.io.Reader reader = new InputStreamReader(inputStream);
        boolean success = false;
        try (com.opencsv.CSVReader csvReader = new CSVReaderBuilder(reader)
                .build()) {
            String[] nextLine = csvReader.readNext();
            if (!validateHeader(inputHeaders, nextLine)) {
                throw new IOException("Invalid Headers arrangement");
            }
            while ((nextLine = csvReader.readNext()) != null) {
                BaseBean row = new BaseBean();
                for (int i = 0; i < inputColumns.length; i++) {
                    row.setString(inputColumns[i], nextLine[i]);
                }
                row.setString("document-id", id);
                if (getRowValidator().validateRow(row, fileBean)) {
                    tables.add(row);
                }
            }
            success = true;
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException(e);
        }
        return success;
    }

    public RowValidator getRowValidator() {
        return rowValidator;
    }

    public void setRowValidator(RowValidator rowValidator) {
        this.rowValidator = rowValidator;
    }

}
