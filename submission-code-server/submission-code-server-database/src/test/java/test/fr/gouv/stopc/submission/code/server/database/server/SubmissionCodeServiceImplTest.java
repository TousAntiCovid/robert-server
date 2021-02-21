package test.fr.gouv.stopc.submission.code.server.database.server;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.entity.SubmissionCode;
import fr.gouv.stopc.submission.code.server.database.repository.SubmissionCodeRepository;
import fr.gouv.stopc.submission.code.server.database.service.impl.SubmissionCodeServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Transactional
public class SubmissionCodeServiceImplTest {
    private SubmissionCodeRepository submissionCodeRepositoryMock = Mockito.mock(SubmissionCodeRepository.class);

    @Test
    public void testGetCodeValidity() {
        SubmissionCode submissionCode = new SubmissionCode();
        Mockito.when(submissionCodeRepositoryMock.findByCodeAndType(Mockito.anyString(),Mockito.anyString())).thenReturn(submissionCode);
        SubmissionCodeServiceImpl submissionCodeServiceImplTest = new SubmissionCodeServiceImpl(submissionCodeRepositoryMock);
        String code= "test";
        Optional<SubmissionCodeDto> result = submissionCodeServiceImplTest.getCodeValidity(code, CodeTypeEnum.LONG);
        Assertions.assertTrue(result.isPresent());
    }

    @Test
    public void testSaveAllCodeGenerateByBatch() {
        List<SubmissionCode> submissionCodes = new ArrayList<>();
        SubmissionCode codePositive = new SubmissionCode();
        submissionCodes.add(codePositive);
        Mockito.when(submissionCodeRepositoryMock.saveAll(Mockito.anyList())).thenReturn(submissionCodes);
        SubmissionCodeServiceImpl submissionCodeServiceTest = new SubmissionCodeServiceImpl(submissionCodeRepositoryMock);
        List<SubmissionCodeDto> submissionCodeDtos = new ArrayList<>();
        SubmissionCodeDto submissionCodeDto = new SubmissionCodeDto();
        submissionCodeDtos.add(submissionCodeDto);
        Iterable<SubmissionCode> result = submissionCodeServiceTest.saveAllCodes(submissionCodeDtos);
        Assertions.assertTrue(StreamSupport.stream(result.spliterator(), false).count() != 0);
    }

    @Test
    public void testSaveCodeGenerate() {
        SubmissionCode submissionCode = new SubmissionCode();
        Mockito.when(submissionCodeRepositoryMock.save(submissionCode)).thenReturn(submissionCode);
        SubmissionCodeServiceImpl submissionCodeServiceTest = new SubmissionCodeServiceImpl(submissionCodeRepositoryMock);
        SubmissionCodeDto submissionCodeDto = new SubmissionCodeDto();
        Optional<SubmissionCode> result= submissionCodeServiceTest.saveCode(submissionCodeDto);
        Assertions.assertTrue(result.isPresent());

    }

    @Test
    public void testGetValidityEmpty(){
        SubmissionCode submissionCode = new SubmissionCode();
        Mockito.when(submissionCodeRepositoryMock.findByCodeAndType(Mockito.anyString(),Mockito.anyString())).thenReturn(submissionCode);
        SubmissionCodeServiceImpl submissionCodeService = new SubmissionCodeServiceImpl(submissionCodeRepositoryMock);
        String code = "";
        Optional<SubmissionCodeDto> result = submissionCodeService.getCodeValidity(code, CodeTypeEnum.LONG);
        Assertions.assertTrue(!result.isPresent());
    }

    @Test
    public void testSaveAllCodeGenerateByBatchEmpty() {
        SubmissionCodeServiceImpl codePositiveServiceTest = new SubmissionCodeServiceImpl(submissionCodeRepositoryMock);
        Iterable<SubmissionCode> result = codePositiveServiceTest.saveAllCodes(new ArrayList<>());
        Assertions.assertNotNull(result);
    }


    @Test
    public void testUpdateCodeUsed(){
        SubmissionCodeDto submissionCodeDto = new SubmissionCodeDto();
        submissionCodeDto.setCode("test");
        submissionCodeDto.setType("test");
        SubmissionCode submissionCode = new SubmissionCode();
        Mockito.when(submissionCodeRepositoryMock.findByCodeAndType(Mockito.anyString(),Mockito.anyString())).thenReturn(submissionCode);
        Mockito.when(submissionCodeRepositoryMock.save(submissionCode)).thenReturn(submissionCode);
        SubmissionCodeServiceImpl submissionCodeService = new SubmissionCodeServiceImpl(submissionCodeRepositoryMock);
        Assertions.assertTrue(submissionCodeService.updateCodeUsed(submissionCodeDto));
    }
    @Test
    public void testUpdateCodeUsedNotFound(){
        SubmissionCodeDto submissionCodeDto = new SubmissionCodeDto();
        submissionCodeDto.setCode("test");
        submissionCodeDto.setType("test");
        SubmissionCode submissionCode = new SubmissionCode();
        Mockito.when(submissionCodeRepositoryMock.findByCodeAndType(Mockito.anyString(),Mockito.anyString())).thenReturn(null);
        Mockito.when(submissionCodeRepositoryMock.save(submissionCode)).thenReturn(submissionCode);
        SubmissionCodeServiceImpl submissionCodeService = new SubmissionCodeServiceImpl(submissionCodeRepositoryMock);
        Assertions.assertTrue(!submissionCodeService.updateCodeUsed(submissionCodeDto));
    }

}
