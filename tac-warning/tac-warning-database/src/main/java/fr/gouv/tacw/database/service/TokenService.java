package fr.gouv.tacw.database.service;

import org.springframework.stereotype.Service;

@Service
public class TokenService {

	public boolean infectedStaticTokensIncludes(String token) {
		return false;
	}
}
