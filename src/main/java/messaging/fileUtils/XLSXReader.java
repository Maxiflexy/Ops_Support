package messaging.fileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import util.BaseBean;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class XLSXReader  implements Reader {

    final static Logger LOG = LogManager.getLogger(XLSXReader.class);

    RowValidator rowValidator;

    @Override
    public boolean readFile(InputStream inputStream, String[] inputColumns, String[] inputHeaders, BaseBean fileBean, List<BaseBean> tables) {
        String id = UUID.randomUUID().toString();
//        fileBean.setString("document-id", id);
        LOG.info("XLSX read file: Document ID: {}", fileBean.getString("document-id") );
        boolean success = false;
        try {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            String[] fileHeader = new String[inputHeaders.length];
            for (Row row : sheet) {
                BaseBean rowBean = new BaseBean();
                for (int i = 0; i < inputHeaders.length; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (row.getRowNum() == 0) {
                        try {
                            fileHeader[i] = cell.getStringCellValue();
                            if (i == inputHeaders.length - 1 && validateHeader(inputHeaders, fileHeader)) {
                                continue;
                            } else if (i == inputHeaders.length - 1) {
                                throw new IOException("Invalid header arrangement");
                            } else {
                                continue;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            throw new IOException("Invalid header arrangement");
                        }

                    }
                    String cellValue = "";
                    switch (cell.getCellType()) {
                        case STRING:
                            cellValue = cell.getStringCellValue();
                            rowBean.setString(inputColumns[cell.getColumnIndex()], cellValue);
                            break;
                        case NUMERIC:
                            cellValue = String.valueOf(cell.getNumericCellValue());
                            rowBean.setString(inputColumns[cell.getColumnIndex()], cellValue);
                            break;
                        case BOOLEAN:
                            cellValue = String.valueOf(cell.getBooleanCellValue());
                            rowBean.setString(inputColumns[cell.getColumnIndex()], cellValue);
                            break;

                        default:
                            throw new IOException("Invalid field at row " + row.getRowNum() + " and column: " + cell.getColumnIndex());
                            // Handle other cell types as

                    }


                }
                if (row.getRowNum() != 0) {
                    rowBean.setString("document-id", id);
                    if (getRowValidator().validateRow(rowBean, fileBean)) {
                        tables.add(rowBean);
                    } else {
                        throw new IllegalArgumentException("Invalid field at row " + row.getRowNum());
                    }
                }

            }
            success = true;
            workbook.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return success;
    }

    @Override
    public RowValidator getRowValidator() {
        return this.rowValidator;
    }

    @Override
    public void setRowValidator(RowValidator rowValidator) {
        this.rowValidator = rowValidator;
    }

}
