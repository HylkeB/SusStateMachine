package io.github.hylkeb.susstatemachine.test.requeststate

import io.github.hylkeb.susstatemachine.StateImpl
import io.github.hylkeb.susstatemachine.Transition
import io.github.hylkeb.susstatemachine.test.Api
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class FetchingStateImpl(
    di: DI
) : StateImpl<RequestState>(), RequestState.Fetching, DIAware by di {

    private val api: Api by instance()

    override suspend fun enter(): Transition<RequestState> {
        val response = api.doApiCall()
            .getOrElse { return Transition(ErrorStateImpl(di, it), "api error", it) }
        return Transition(SuccessStateImpl(response))
    }

}
