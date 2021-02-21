package fr.gouv.stopc.submission.code.server.ws.controller.error;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;


@Getter
@Setter
public class SubmissionCodeServerException extends Exception {


    private ExceptionEnum serverExceptionEnum;

    private final LocalDateTime timestamp;

    private final String debugMessage;

    public enum ExceptionEnum {
        NUMBER_OF_TRIES_REACHED_ERROR("Number of try generating code has been reached."),
        DB_NO_RECORD_FOR_LOT_IDENTIFIER_ERROR("Lot identifier has no record."),
        DB_INVALID_PARAMETERS_ERROR("The given parameter does not allow to give back any further information."),
        CODE_GENERATION_FAILED_ERROR("Server cannot create codes."),
        INVALID_CODE_TYPE_ERROR("The code type is not supported yet."),
        VALIDATION_FIELD_ERROR("Encounters error on field validation control."),
        UNQUALIFIED_ERROR("Unqualified error."),
        PACKAGING_CSV_FILE_ERROR("An error occurs during packaging csv file in zip."),
        CODE_TO_CSV_PARSING_ERROR("Submission codes list has failed to be parsed into CSV format."),
        CODE_TO_CSV_UTF8_ENCODING_ERROR("Submission codes csv file has failed to be encoded in UTF-8."),
        SFTP_CONNECTION_FAILED_ERROR("The attempt at connecting to SFTP server has failed."),
        SFTP_FILE_PUSHING_FAILED_ERROR("The attempt at writing file through SFTP connection has failed."),
        JSCH_SESSION_CREATION_FAILED_ERROR("The attempt at creating JSCH session has failed."),
        INVALID_DATE("The date validation has failed."),
        PARSE_STR_DATE_ERROR("An error occurred when parsing date"),
        DB_SAVE_OPTIONAL_EMPTY("The data save action has not raised error but returned a empty Optional."),
        SFTP_WORKING_DIRECTORY_ERROR("Change default working directory on chanelSFTP failed."),
        MAPPING_CODE_FOR_CSV_FILE_ERROR("An error occurs at mapping code for csv file creation.");

        @Getter
        private final String message;

        ExceptionEnum(final String message) {
            this.message = message;
        }

    }

    public SubmissionCodeServerException() {
        this.timestamp = LocalDateTime.now();
        this.debugMessage = null;
        this.serverExceptionEnum=null;
    }

    @Override
    public String getLocalizedMessage() {
        return this.serverExceptionEnum.message;
    }

    public SubmissionCodeServerException(ExceptionEnum serverExceptionEnum) {
        this(serverExceptionEnum, null);
    }


    public SubmissionCodeServerException(ExceptionEnum serverExceptionEnum, Throwable ex) {
        super(serverExceptionEnum.message, ex);
        this.serverExceptionEnum = serverExceptionEnum;

        this.debugMessage = ex!=null ? ex.getLocalizedMessage() : null;
       
        this.timestamp= LocalDateTime.now();
    }

    public SimpleErrorBody body() {
        final SimpleErrorBody body = new SimpleErrorBody();
        new ModelMapper().map(this, body);
        return body;
    }

    public SimpleErrorBody body(String detail) {
        final DetailedErrorBody body = new DetailedErrorBody();
        new ModelMapper().map(this, body);
        body.setDetail(detail);
        return body;
    }

    @Getter
    @Setter
    public static class SimpleErrorBody {
        private LocalDateTime timestamp;
        private ExceptionEnum codeException;
    }

    @Getter
    @Setter
    public static class DetailedErrorBody extends SimpleErrorBody{
        private String detail;
    }

}
