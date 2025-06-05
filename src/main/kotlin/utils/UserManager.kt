package utils

import data.model.JwtResponse

/**
 * Manages user information throughout the application.
 * This class stores the JwtResponse object, which contains the user's information,
 * including the username, email, and roles.
 */
object UserManager {
    private var currentUser: JwtResponse? = null

    /**
     * Sets the current user.
     *
     * @param user The JwtResponse object containing the user's information
     */
    fun setUser(user: JwtResponse?) {
        currentUser = user
    }

    /**
     * Gets the current user.
     *
     * @return The JwtResponse object containing the user's information, or null if not set
     */
    fun getUser(): JwtResponse? {
        return currentUser
    }

    /**
     * Gets the current user's username.
     *
     * @return The username of the current user, or "Unknown User" if not set
     */
    fun getUsername(): String {
        return currentUser?.username ?: "Unknown User"
    }

    /**
     * Gets the current user's ID.
     *
     * @return The ID of the current user, or -1 if not set
     */
    fun getUserId(): Long {
        return currentUser?.id ?: -1
    }

    /**
     * Checks if a user is currently set.
     *
     * @return True if a user is set, false otherwise
     */
    fun hasUser(): Boolean {
        return currentUser != null
    }

    /**
     * Clears the current user.
     */
    fun clearUser() {
        currentUser = null
    }
}