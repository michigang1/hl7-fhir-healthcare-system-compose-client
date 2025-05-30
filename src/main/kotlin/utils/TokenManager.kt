package utils

object TokenManager {
    private var currentToken: String? = null

    fun setToken(token: String?) {
        currentToken = token
    }

    fun getToken(): String? {
        return currentToken
    }

    fun hasToken(): Boolean {
        return currentToken != null
    }

    fun clearToken() {
        currentToken = null
    }
}