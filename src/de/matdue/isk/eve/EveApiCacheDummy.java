package de.matdue.isk.eve;

/**
 * Dummy implementation of EveApiCache
 * which does not cache anything and reports each key as uncached.
 */
public class EveApiCacheDummy implements EveApiCache {

	@Override
	public boolean isCached(String key) {
		return false;
	}

	@Override
	public void cache(String key, CacheInformation cacheInformation) {
	}

	@Override
	public void urlAccessed(String url, String keyID, String result) {
	}

}
