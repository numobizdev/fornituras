package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.config.QrProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class QrCodeGeneratorService {

	private final QrProperties qrProperties;
	private final Pattern codePattern;

	public QrCodeGeneratorService(QrProperties qrProperties) {
		this.qrProperties = qrProperties;
		this.codePattern = Pattern.compile(
				"^" + Pattern.quote(qrProperties.prefix()) + "\\d{" + qrProperties.sequenceLength() + "}$");
	}

	public String formatCode(int consecutivo) {
		validateConsecutivo(consecutivo);
		return qrProperties.prefix() + String.format("%0" + qrProperties.sequenceLength() + "d", consecutivo);
	}

	public List<String> formatRange(int consecutivoInicial, int consecutivoFinal) {
		if (consecutivoFinal < consecutivoInicial) {
			throw new BadRequestException("Invalid consecutive range");
		}

		int count = consecutivoFinal - consecutivoInicial + 1;
		List<String> codes = new ArrayList<>(count);
		for (int consecutivo = consecutivoInicial; consecutivo <= consecutivoFinal; consecutivo++) {
			codes.add(formatCode(consecutivo));
		}
		return codes;
	}

	public int maxConsecutivo() {
		int length = qrProperties.sequenceLength();
		if (length >= 10) {
			return Integer.MAX_VALUE;
		}
		return (int) Math.pow(10, length) - 1;
	}

	public boolean isValidFormat(String code) {
		return code != null && codePattern.matcher(code).matches();
	}

	private void validateConsecutivo(int consecutivo) {
		if (consecutivo < 1 || consecutivo > maxConsecutivo()) {
			throw new BadRequestException("Consecutive number out of allowed range");
		}
	}
}
