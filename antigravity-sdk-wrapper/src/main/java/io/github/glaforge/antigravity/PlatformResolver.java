package io.github.glaforge.antigravity;

public class PlatformResolver {
	public static String getPlatformSlice() {
		String os = System.getProperty("os.name").toLowerCase();
		String arch = System.getProperty("os.arch").toLowerCase();

		String osPart;
		if (os.contains("linux"))
			osPart = "linux";
		else if (os.contains("mac") || os.contains("darwin"))
			osPart = "osx";
		else if (os.contains("windows"))
			osPart = "windows";
		else
			throw new IllegalStateException("Unsupported OS: " + os);

		String archPart;
		if (arch.contains("amd64") || arch.contains("x86_64"))
			archPart = "x86_64";
		else if (arch.contains("aarch64") || arch.contains("arm64"))
			archPart = "aarch64";
		else
			throw new IllegalStateException("Unsupported Architecture: " + arch);

		return osPart + "-" + archPart;
	}
}
