package pt.amn.moveon.presentation.viewmodels.utils

data class Resource<out T>(val status: LoadStatus, val data: T?, val message: String?) {

    companion object {

        fun <T> success(data: T?): Resource<T> {
            return Resource(LoadStatus.SUCCESS, data, null)
        }

        fun <T> error(msg: String?, data: T?): Resource<T> {
            return Resource(LoadStatus.ERROR, data, msg)
        }

        fun <T> loading(data: T?): Resource<T> {
            return Resource(LoadStatus.LOADING, data, null)
        }

    }

}

enum class LoadStatus {
    SUCCESS,
    ERROR,
    LOADING
}