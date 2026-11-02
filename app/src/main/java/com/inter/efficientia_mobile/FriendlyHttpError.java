package com.inter.efficientia_mobile;

import android.content.Context;

/** Maps response codes to actions a driver can understand. The code stays in diagnostics. */
public final class FriendlyHttpError {
    private FriendlyHttpError() { }

    public static String message(Context context, int status) {
        int message;
        switch (status) {
            case 400: message = R.string.error_bad_request; break;
            case 401: message = R.string.error_session_expired; break;
            case 403: message = R.string.error_access_denied; break;
            case 404: message = R.string.error_not_found; break;
            case 409: message = R.string.error_conflict; break;
            case 413: message = R.string.error_too_large; break;
            case 429: message = R.string.error_too_many_requests; break;
            case 500: case 502: case 503: case 504:
                message = R.string.error_service_unavailable; break;
            default: message = R.string.error_unexpected; break;
        }
        return context.getString(message);
    }
}
