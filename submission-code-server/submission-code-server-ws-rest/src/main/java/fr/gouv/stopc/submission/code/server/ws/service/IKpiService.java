package fr.gouv.stopc.submission.code.server.ws.service;

import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.vo.SubmissionCodeServerKpi;

import java.time.LocalDate;
import java.util.List;

public interface IKpiService {

    List<SubmissionCodeServerKpi> generateKPI(LocalDate dateFrom, LocalDate dateTo) throws SubmissionCodeServerException;
}
