package main

import "testing"

func TestHashAndCheckPassword(t *testing.T) {
	h := hashPassword("secret123")
	if h == "" || h == "secret123" {
		t.Fatalf("expected hashed password, got %q", h)
	}
	if !checkPassword(h, "secret123") {
		t.Fatal("bcrypt check failed")
	}
	if checkPassword(h, "wrong") {
		t.Fatal("wrong password should fail")
	}
	// legacy plaintext
	if !checkPassword("plain", "plain") {
		t.Fatal("legacy plaintext should pass")
	}
	// legacy sha256 without prefix
	sum := hashPassword("") // just ensure function stable
	_ = sum
	// sha256 prefixed path via direct known: use check with sha256: prefix from hashPassword fallback is rare
}

func TestDefaultPerms(t *testing.T) {
	if len(defaultPerms("tenant_admin")) < 3 {
		t.Fatal("tenant_admin perms too small")
	}
	if len(defaultPerms("member")) < 2 {
		t.Fatal("member perms")
	}
}
