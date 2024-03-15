package io.github.hylkeb.susstatemachine.test

/**
 * Very simple api interface to simulate api calls
 */
interface Api {
    suspend fun doApiCall(): Result<String>
}
