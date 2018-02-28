package com.ultical.dfvmv;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.function.Function;

import ratpack.error.ServerErrorHandler;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.RequestLogger;
import ratpack.http.Status;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;

public class Main {

	private static ServerConfigBuilder configureDefaults(ServerConfigBuilder builder) {
		builder.port(8080).findBaseDir().development(true);
		return builder;
	}

	public static void main(String... args) throws Exception {
		ServerConfig config = configureDefaults(ServerConfig.builder()).args(args).build();
		RatpackServer.start(server -> server.serverConfig(config).handlers(chain -> {
			chain.register(r -> r.add(ServerErrorHandler.class, (ctx, throwable) -> {
				if (throwable instanceof NoSuchFileException) {
					ctx.clientError(404);
				} else {
					ctx.getResponse().status(Status.of(500, "Internal Server Error"));
				}
			}));
			chain.all(RequestLogger.ncsa())
					.get("profile/sparte/ultimate", new ReturnFileHandler(ctx -> "all-profiles.json"))
					.get("profil/:id", new ReturnFileHandler(ctx -> {
						final String profileId = ctx.getPathTokens().get("id");
						return profileId + ".json";
					}))
					.get("verbaende", new ReturnFileHandler(ctx -> "association.json"))
					.get("vereine", new ReturnFileHandler(ctx -> "clubs.json"));
		}));
	}

	public static class ReturnFileHandler implements Handler {

		private final Function<Context, String> provider;

		public ReturnFileHandler(Function<Context, String> prov) {
			this.provider = prov;
		}

		@Override
		public void handle(Context ctx) throws Exception {
			try {
				String fileName = this.provider.apply(ctx);
				if (fileName != null && !fileName.trim().isEmpty()) {
					final Path filePath = ctx.file(fileName);
					ctx.getResponse().status(200).send("application/json",
							new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8));
				} else {

					ctx.clientError(404);
				}

			} catch (Exception ex) {
				ctx.error(ex);
			}
		}
	}
}
