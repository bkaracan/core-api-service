package com.enterprise.coreapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class CoreApiServiceApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(CoreApiServiceApplication.class, args);
	}

	/**
	 * Proje kök dizinindeki .env dosyasını otomatik olarak okur ve
	 * ortam değişkeni olarak tanımlanmamışsa System Properties havuzuna ekler.
	 */
	private static void loadDotenv() {
		Path[] possiblePaths = new Path[] {
				Paths.get(".env"),
				Paths.get("..", ".env"),
				Paths.get("core-api-service", ".env")
		};

		for (Path path : possiblePaths) {
			if (Files.exists(path)) {
				try {
					List<String> lines = Files.readAllLines(path);
					for (String line : lines) {
						String trimmed = line.trim();
						if (trimmed.isEmpty() || trimmed.startsWith("#")) {
							continue;
						}
						int eqIdx = trimmed.indexOf('=');
						if (eqIdx > 0) {
							String key = trimmed.substring(0, eqIdx).trim();
							String val = trimmed.substring(eqIdx + 1).trim();
							if (System.getProperty(key) == null && System.getenv(key) == null) {
								System.setProperty(key, val);
							}
						}
					}
					break;
				} catch (IOException ignored) {}
			}
		}
	}

}
