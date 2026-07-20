package main

import "testing"

func TestMatchTenant(t *testing.T) {
	if !matchTenant(0, 99) {
		t.Fatal("tid=0 should match all")
	}
	if !matchTenant(1, 0) {
		t.Fatal("legacy row belongs to demo tenant 1")
	}
	if matchTenant(2, 0) {
		t.Fatal("legacy row must not leak to tenant 2")
	}
	if !matchTenant(2, 2) {
		t.Fatal("same tenant")
	}
	if matchTenant(2, 1) {
		t.Fatal("cross tenant")
	}
}
