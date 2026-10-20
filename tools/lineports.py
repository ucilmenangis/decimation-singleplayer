#!/usr/bin/env python3
"""Ports of this checkout's dev games, so main and the prerelease worktree each run their own live test game.

    python3 tools/lineports.py        # prints: branch, live test port, debug port

main (multiplayer) and prerelease (plain fixes) are two checkouts of one repo (CLAUDE.md "Repository"). Each line's
live test game (tools/devtest.py --live, devtest/DevTestLive) and debug port (-Photswap / -Plive, tools/hotswap.py)
gets its own number, so a live game of one line never answers the other line's runs. main's second game on this Mac
(the production client as a joiner, docs/player_hosting.md) keeps 25598 / 5006.
"""
import os
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT = (25599, 5005)
PORTS = {"prerelease": (25597, 5007)}


def branch():
    try:
        return subprocess.run(["git", "rev-parse", "--abbrev-ref", "HEAD"], cwd=ROOT, capture_output=True,
                              text=True).stdout.strip()
    except OSError:
        return ""


def ports(name=None):
    """(live test port, debug port) of a branch, this checkout's by default."""
    return PORTS.get(branch() if name is None else name, DEFAULT)


if __name__ == "__main__":
    live, debug = ports()
    print("%s live %d debug %d" % (branch() or "?", live, debug))
