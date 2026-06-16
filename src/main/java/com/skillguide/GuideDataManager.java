package com.skillguide;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.skillguide.model.GuideData;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Responsible for obtaining {@link GuideData}.
 *
 * Strategy (in priority order):
 *   1. Fetch fresh JSON from the configured URL (asynchronously) and cache it to disk.
 *   2. If the network fails: use the last cached file on disk.
 *   3. If no cache exists: use the bundled guide-data.json from the plugin jar.
 *
 * This class is what makes the guide "self-updating": the data lives outside the code,
 * so the guide can be updated without a new plugin version.
 */
@Slf4j
@Singleton
public class GuideDataManager
{
	private static final File CACHE_FILE = new File(RuneLite.RUNELITE_DIR, "skill-guide-cache.json");
	private static final String BUNDLED_RESOURCE = "/com/skillguide/guide-data.json";

	private final OkHttpClient okHttpClient;
	private final Gson gson;

	@Getter
	private GuideData guideData;

	@Inject
	public GuideDataManager(OkHttpClient okHttpClient, Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	/**
	 * Immediately loads the best local data (cache or bundled) so the UI is never empty.
	 * Call this on start-up before any network fetch.
	 */
	public void loadLocal()
	{
		GuideData local = readFromCache();
		if (local == null)
		{
			local = readBundled();
		}
		if (local != null)
		{
			guideData = local;
		}
	}

	/**
	 * Fetches the latest guide from the URL in the background. On success {@link #guideData} is updated,
	 * the file is cached, and {@code onUpdated} is called on OkHttp's thread (the UI must hop to the EDT itself).
	 */
	public void fetchRemote(String url, Consumer<GuideData> onUpdated)
	{
		Request request = new Request.Builder().url(url).build();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Could not fetch skill guide from {} - using local copy", url, e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (Response r = response)
				{
					if (!r.isSuccessful() || r.body() == null)
					{
						log.warn("Unexpected response when fetching skill guide: {}", r.code());
						return;
					}

					String json = r.body().string();
					GuideData fresh = gson.fromJson(json, GuideData.class);
					if (fresh == null)
					{
						return;
					}

					guideData = fresh;
					writeCache(json);
					onUpdated.accept(fresh);
				}
				catch (JsonSyntaxException e)
				{
					log.warn("Invalid JSON in remote skill guide", e);
				}
			}
		});
	}

	private GuideData readFromCache()
	{
		if (!CACHE_FILE.exists())
		{
			return null;
		}
		try
		{
			String json = new String(Files.readAllBytes(CACHE_FILE.toPath()), StandardCharsets.UTF_8);
			return gson.fromJson(json, GuideData.class);
		}
		catch (IOException | JsonSyntaxException e)
		{
			log.warn("Could not read cached skill guide", e);
			return null;
		}
	}

	private GuideData readBundled()
	{
		try (InputStream in = GuideDataManager.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			if (in == null)
			{
				log.warn("Bundled guide-data.json was not found");
				return null;
			}
			return gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), GuideData.class);
		}
		catch (IOException | JsonSyntaxException e)
		{
			log.warn("Could not read bundled skill guide", e);
			return null;
		}
	}

	private void writeCache(String json)
	{
		try
		{
			Files.write(CACHE_FILE.toPath(), json.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.warn("Could not cache skill guide to disk", e);
		}
	}
}
