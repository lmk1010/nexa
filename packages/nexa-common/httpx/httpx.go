package httpx

import (
	"encoding/json"
	"net/http"
)

type Body map[string]any

func JSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func OK(w http.ResponseWriter, data any) {
	JSON(w, http.StatusOK, Body{"code": 0, "data": data})
}

func OKList(w http.ResponseWriter, data any, total int) {
	JSON(w, http.StatusOK, Body{"code": 0, "data": data, "total": total})
}

func Fail(w http.ResponseWriter, status int, code int, msg string) {
	JSON(w, status, Body{"code": code, "msg": msg})
}

func DecodeJSON(r *http.Request, dst any) error {
	defer r.Body.Close()
	return json.NewDecoder(r.Body).Decode(dst)
}
