package com.cozy.notebooks.service;

public final class UserActivityActions {

    public static final String AUTH_REGISTER = "auth.register";
    public static final String AUTH_LOGIN = "auth.login";
    public static final String AUTH_GOOGLE_LOGIN = "auth.google_login";
    public static final String AUTH_REFRESH = "auth.refresh";
    public static final String AUTH_LOGOUT = "auth.logout";

    public static final String NOTEBOOK_CREATE = "notebook.create";
    public static final String NOTEBOOK_UPDATE = "notebook.update";
    public static final String NOTEBOOK_DELETE = "notebook.delete";

    public static final String PAGE_CREATE = "page.create";
    public static final String PAGE_UPDATE = "page.update";
    public static final String PAGE_DELETE = "page.delete";

    public static final String TEMPLATE_CREATE = "template.create";
    public static final String TEMPLATE_INSTANTIATE = "template.instantiate";

    public static final String FEEDBACK_CREATE = "feedback.create";

    public static final String QUOTA_EXCEEDED = "quota.exceeded";

    private UserActivityActions() {
    }
}
