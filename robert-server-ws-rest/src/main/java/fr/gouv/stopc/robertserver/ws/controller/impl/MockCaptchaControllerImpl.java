package fr.gouv.stopc.robertserver.ws.controller.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.gouv.stopc.robertserver.ws.controller.CaptchaController;
import fr.gouv.stopc.robertserver.ws.dto.CaptchaCreationDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.vo.CaptchaCreationVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "captcha.gateway.enabled", havingValue = "false")
public class MockCaptchaControllerImpl implements CaptchaController {

	@Override
	public ResponseEntity<CaptchaCreationDto> createCaptcha(@Valid CaptchaCreationVo captchaCreationVo) {

		CaptchaCreationDto dto = new CaptchaCreationDto();
		dto.setCaptchaId("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		ResponseEntity<CaptchaCreationDto> response = ResponseEntity.ok().body(dto);

		return response;
	}

	@Override
	public ResponseEntity<byte[]> getCaptchaImage(String captchaId) throws RobertServerException {
		return this.getCaptchaCommon(captchaId, "image");
	}

	@Override
	public ResponseEntity<byte[]> getCaptchaAudio(String captchaId) throws RobertServerException {
		return this.getCaptchaCommon(captchaId, "audio");
	}

	protected static final int SAMPLE_RATE = 16 * 1024;

	private ResponseEntity<byte[]> getCaptchaCommon(String captchaId, String mediaType) {
		log.info("Getting captcha {} as {}", captchaId, mediaType);
		if ("audio".equals(mediaType)) {
			final double sampleRate = 44100.0;
	        final double frequency = 440;
	        final double frequency2 = 90;
	        final double amplitude = 1.0;
	        final double seconds = 2.0;
	        final double twoPiF = 2 * Math.PI * frequency;
	        final double piF = Math.PI * frequency2;

	        float[] buffer = new float[(int)(seconds * sampleRate)];

	        for (int sample = 0; sample < buffer.length; sample++) {
	            double time = sample / sampleRate;
	            buffer[sample] = (float)(amplitude * Math.cos(piF * time) * Math.sin(twoPiF * time));
	        }

	        final byte[] byteBuffer = new byte[buffer.length * 2];

	        int bufferIndex = 0;
	        for (int i = 0; i < byteBuffer.length; i++) {
	            final int x = (int)(buffer[bufferIndex++] * 32767.0);

	            byteBuffer[i++] = (byte)x;
	            byteBuffer[i] = (byte)(x >>> 8);
	        }

	        final boolean bigEndian = false;
	        final boolean signed = true;

	        final int bits = 16;
	        final int channels = 1;

	        AudioFormat format = new AudioFormat((float)sampleRate, bits, channels, signed, bigEndian);
	        ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
	        AudioInputStream audioInputStream = new AudioInputStream(bais, format, buffer.length);
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        try {
				AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, baos);
			} catch (IOException e) {
				e.printStackTrace();
			}
	        try {
				audioInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

	        byte[] bytes = baos.toByteArray();			
			ResponseEntity<byte[]> response = ResponseEntity.ok().body(bytes);
			return response;
			
		}
		/*byte[] aByteArray = { 0xa, 0x2, 0xf, (byte) 0xff, (byte) 0xff, (byte) 0xff };
		int width = 1;
		int height = 2;

		DataBuffer buffer = new DataBufferByte(aByteArray, aByteArray.length);

		WritableRaster raster = Raster.createInterleavedRaster(buffer, width, height, 3 * width, 3,
				new int[] { 0, 1, 2 }, (Point) null);
		ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true,
				Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		BufferedImage image = new BufferedImage(cm, raster, true, null);*/
		// Will be inject through volume in the docker
		BufferedImage image= null;
		try {
			image = ImageIO.read(new File("/tmp/CC.png"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			if (image != null)
			ImageIO.write(image, "png", baos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] bytes = baos.toByteArray();
		ResponseEntity<byte[]> response = ResponseEntity.ok().body(bytes);
		return response;
	}

}
