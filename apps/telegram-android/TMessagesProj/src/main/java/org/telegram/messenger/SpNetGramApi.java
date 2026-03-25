package org.telegram.messenger;

import org.json.JSONObject;
import org.telegram.ui.web.HttpGetTask;
import org.telegram.ui.web.HttpPostTask;

public class SpNetGramApi {

    private static JSONObject parse(String response) {
        if (response == null) {
            return null;
        }
        String trimmed = response.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(trimmed);
        } catch (Exception e) {
            try {
                JSONObject fallback = new JSONObject();
                fallback.put("error", trimmed);
                return fallback;
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    private static String buildUrl(String path) {
        String base = SpNetGramConfig.getBackendBase();
        if (base == null || base.trim().isEmpty()) {
            base = SpNetGramConfig.BACKEND_URL;
        }
        base = base.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/api") && path.startsWith("/api/")) {
            base = base.substring(0, base.length() - 4);
        }
        return base + path;
    }

    public static void login(String email, String password, Utilities.Callback<JSONObject> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("email", email);
            payload.put("password", password);
            new HttpPostTask("application/json", payload.toString(), result -> {
                if (callback != null) {
                    callback.run(parse(result));
                }
            }).execute(buildUrl("/api/auth/login"));
        } catch (Exception e) {
            if (callback != null) {
                callback.run(null);
            }
        }
    }

    public static void register(String email, String password, String displayName, Utilities.Callback<JSONObject> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("email", email);
            payload.put("password", password);
            payload.put("displayName", displayName);
            new HttpPostTask("application/json", payload.toString(), result -> {
                if (callback != null) {
                    callback.run(parse(result));
                }
            }).execute(buildUrl("/api/auth/register"));
        } catch (Exception e) {
            if (callback != null) {
                callback.run(null);
            }
        }
    }

    public static void requestPasswordReset(String email, Utilities.Callback<JSONObject> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("email", email);
            new HttpPostTask("application/json", payload.toString(), result -> {
                if (callback != null) {
                    callback.run(parse(result));
                }
            }).execute(buildUrl("/api/auth/reset/request"));
        } catch (Exception e) {
            if (callback != null) {
                callback.run(null);
            }
        }
    }

    public static void confirmPasswordReset(String token, String newPassword, Utilities.Callback<JSONObject> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("token", token);
            payload.put("newPassword", newPassword);
            new HttpPostTask("application/json", payload.toString(), result -> {
                if (callback != null) {
                    callback.run(parse(result));
                }
            }).execute(buildUrl("/api/auth/reset/confirm"));
        } catch (Exception e) {
            if (callback != null) {
                callback.run(null);
            }
        }
    }

    public static void accessStatus(String token, Utilities.Callback<JSONObject> callback) {
        HttpGetTask task = new HttpGetTask(result -> {
            if (callback != null) {
                callback.run(parse(result));
            }
        });
        if (token != null && !token.isEmpty()) {
            task.setHeader("Authorization", "Bearer " + token);
        }
        task.execute(buildUrl("/api/access/status"));
    }

    public static void health(Utilities.Callback<JSONObject> callback) {
        HttpGetTask task = new HttpGetTask(result -> {
            if (callback != null) {
                callback.run(parse(result));
            }
        });
        task.execute(buildUrl("/api/health"));
    }

    public static void redeemLicense(String token, String licenseKey, Utilities.Callback<JSONObject> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("licenseKey", licenseKey);
            HttpPostTask task = new HttpPostTask("application/json", payload.toString(), result -> {
                if (callback != null) {
                    callback.run(parse(result));
                }
            });
            if (token != null && !token.isEmpty()) {
                task.setHeader("Authorization", "Bearer " + token);
            }
            task.execute(buildUrl("/api/license/redeem"));
        } catch (Exception e) {
            if (callback != null) {
                callback.run(null);
            }
        }
    }

    public static void getProfile(String token, Utilities.Callback<JSONObject> callback) {
        HttpGetTask task = new HttpGetTask(result -> {
            if (callback != null) {
                callback.run(parse(result));
            }
        });
        if (token != null && !token.isEmpty()) {
            task.setHeader("Authorization", "Bearer " + token);
        }
        task.execute(buildUrl("/api/profile"));
    }

    public static void mintSpgId(String token, Utilities.Callback<JSONObject> callback) {
        try {
            HttpPostTask task = new HttpPostTask("application/json", "{}", result -> {
                if (callback != null) {
                    callback.run(parse(result));
                }
            });
            if (token != null && !token.isEmpty()) {
                task.setHeader("Authorization", "Bearer " + token);
            }
            task.execute(buildUrl("/api/profile/spg-id/mint"));
        } catch (Exception e) {
            if (callback != null) {
                callback.run(null);
            }
        }
    }

    public static void premiumStatus(String token, Utilities.Callback<JSONObject> callback) {
        HttpGetTask task = new HttpGetTask(result -> {
            if (callback != null) {
                callback.run(parse(result));
            }
        });
        if (token != null && !token.isEmpty()) {
            task.setHeader("Authorization", "Bearer " + token);
        }
        task.execute(buildUrl("/api/premium/status"));
    }

    public static void walletStatus(String token, Utilities.Callback<JSONObject> callback) {
        HttpGetTask task = new HttpGetTask(result -> {
            if (callback != null) {
                callback.run(parse(result));
            }
        });
        if (token != null && !token.isEmpty()) {
            task.setHeader("Authorization", "Bearer " + token);
        }
        task.execute(buildUrl("/api/wallet"));
    }

    public static void claimAirdrop(String token, Utilities.Callback<JSONObject> callback) {
        try {
            HttpPostTask task = new HttpPostTask("application/json", "{}", result -> {
                if (callback != null) {
                    callback.run(parse(result));
                }
            });
            if (token != null && !token.isEmpty()) {
                task.setHeader("Authorization", "Bearer " + token);
            }
            task.execute(buildUrl("/api/wallet/airdrop/claim"));
        } catch (Exception e) {
            if (callback != null) {
                callback.run(null);
            }
        }
    }

    public static void claimGems(String token, Utilities.Callback<JSONObject> callback) {
        try {
            HttpPostTask task = new HttpPostTask("application/json", "{}", result -> {
                if (callback != null) {
                    callback.run(parse(result));
                }
            });
            if (token != null && !token.isEmpty()) {
                task.setHeader("Authorization", "Bearer " + token);
            }
            task.execute(buildUrl("/api/wallet/gems/claim"));
        } catch (Exception e) {
            if (callback != null) {
                callback.run(null);
            }
        }
    }

    public static void assistantChat(String token, String message, String intent, Utilities.Callback<JSONObject> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("intent", intent);
            org.json.JSONArray messages = new org.json.JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", message);
            messages.put(msg);
            payload.put("messages", messages);
            HttpPostTask task = new HttpPostTask("application/json", payload.toString(), result -> {
                if (callback != null) {
                    callback.run(parse(result));
                }
            });
            if (token != null && !token.isEmpty()) {
                task.setHeader("Authorization", "Bearer " + token);
            }
            task.execute(buildUrl("/api/assistant/chat"));
        } catch (Exception e) {
            if (callback != null) {
                callback.run(null);
            }
        }
    }
}
