package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.config.QrProperties;
import com.numobiz.solutions.fornituras.modules.qrcodes.repository.CodigoQrRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class QrCodeGeneratorService {

	private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final Pattern CODE_PATTERN = Pattern.compile("^FOR-[0-9A-Z]{5}$");
	private static final int UNIQUENESS_CHECK_CHUNK_SIZE = 1000;

	private final QrProperties qrProperties;
	private final CodigoQrRepository codigoQrRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public QrCodeGeneratorService(QrProperties qrProperties, CodigoQrRepository codigoQrRepository) {
		this.qrProperties = qrProperties;
		this.codigoQrRepository = codigoQrRepository;
	}

	public String generateUniqueCode(Set<String> batchCodes) {
		return generateUniqueCodes(1, batchCodes).getFirst();
	}

	public List<String> generateUniqueCodes(int count) {
		return generateUniqueCodes(count, Set.of());
	}

	public List<String> generateUniqueCodes(int count, Set<String> reservedCodes) {
		if (count < 1) {
			throw new BadRequestException("At least one QR code is required");
		}

		Set<String> assigned = new HashSet<>(reservedCodes);
		List<String> generated = new ArrayList<>(count);
		int attempts = 0;
		int maxAttempts = Math.max(qrProperties.maxGenerationRetries(), count * 2);

		while (generated.size() < count) {
			if (attempts++ >= maxAttempts) {
				throw new BadRequestException("Unable to generate unique QR codes. Please try again.");
			}

			int remaining = count - generated.size();
			int candidateCount = Math.min(Math.max(remaining * 2, remaining + 16), UNIQUENESS_CHECK_CHUNK_SIZE);
			Set<String> candidates = new HashSet<>(candidateCount);

			while (candidates.size() < candidateCount) {
				candidates.add(buildCode());
			}
			candidates.removeAll(assigned);

			if (candidates.isEmpty()) {
				continue;
			}

			Set<String> existingInDatabase = findExistingInDatabase(candidates);
			for (String candidate : candidates) {
				if (generated.size() >= count) {
					break;
				}
				if (!existingInDatabase.contains(candidate)) {
					generated.add(candidate);
					assigned.add(candidate);
				}
			}
		}

		return generated;
	}

	public boolean isValidFormat(String code) {
		return CODE_PATTERN.matcher(code).matches();
	}

	private Set<String> findExistingInDatabase(Set<String> candidates) {
		if (candidates.isEmpty()) {
			return Set.of();
		}

		Set<String> existing = new HashSet<>();
		List<String> candidateList = new ArrayList<>(candidates);
		for (int index = 0; index < candidateList.size(); index += UNIQUENESS_CHECK_CHUNK_SIZE) {
			int end = Math.min(index + UNIQUENESS_CHECK_CHUNK_SIZE, candidateList.size());
			existing.addAll(codigoQrRepository.findExistingCodigosIn(candidateList.subList(index, end)));
		}
		return existing;
	}

	private String buildCode() {
		StringBuilder suffix = new StringBuilder(qrProperties.suffixLength());
		for (int i = 0; i < qrProperties.suffixLength(); i++) {
			suffix.append(CHARSET.charAt(secureRandom.nextInt(CHARSET.length())));
		}
		return qrProperties.prefix() + suffix;
	}
}
