#!/usr/bin/env bash
# ==============================================================
#  SDN DEMO LAUNCHER — Ryu Controller + Mininet + JavaFX Client
#  Author : Pethu Kannan G
#  Description : One-click demo for Software Defined Networking
#                (Dynamic topology + Ryu + Java-based visualizer)
# ==============================================================

set -euo pipefail

SESSION="sdn_demo"
CTRL_IP="127.0.0.1"
CTRL_OF_PORT="6633"
CTRL_REST_URL="http://127.0.0.1:8080"
PROJECT_DIR="${PROJECT_DIR:-$PWD}"

# --------------------------------------------------------------
# 🧠 Helper function: check dependencies
# --------------------------------------------------------------
need() {
  command -v "$1" >/dev/null 2>&1 || { echo "❌ Missing dependency: $1"; exit 1; }
}

echo "🔍 Checking dependencies..."
need tmux
need ryu-manager
need sudo
need mn
need mvn
need java
need jq || echo "⚠️ jq not found — REST output won't be pretty-printed"

sudo mn -c >/dev/null 2>&1 || true

# --------------------------------------------------------------
# 🧱 Banner
# --------------------------------------------------------------
clear
echo "=============================================================="
echo "              🧠 SOFTWARE DEFINED NETWORKING DEMO             "
echo "--------------------------------------------------------------"
echo "  Controller : Ryu (Python-based OpenFlow Controller)"
echo "  Emulator   : Mininet (Virtual network of switches + hosts)"
echo "  Client     : Java (CLI + JavaFX Visualization)"
echo "--------------------------------------------------------------"
echo " This demo shows centralized SDN control and live topology view"
echo "=============================================================="
echo
echo "Choose a topology type to launch:"
echo
echo "  [1] 🏗️  Tree Topology"
echo "      - Multiple switch layers (depth & fanout)"
echo "      - Example: depth=2, fanout=3 → 4 switches, 9 hosts"
echo
echo "  [2] 🔗  Linear Topology"
echo "      - Switches connected in a line (chain)"
echo "      - Example: 4 switches → h1-s1-s2-s3-s4-h4"
echo
echo "  [3] ⚙️  Single Switch"
echo "      - One switch connected to several hosts"
echo "      - Example: 1 switch, 5 hosts"
echo
read -p "👉 Enter your choice [1-3]: " choice

# --------------------------------------------------------------
# 📐 Topology Configuration
# --------------------------------------------------------------
case $choice in
  1)
    echo
    echo "🏗️  You selected Tree Topology"
    read -p "   → Enter tree depth (levels of switches): " DEPTH
    read -p "   → Enter fanout (hosts per lower switch): " FANOUT
    TOPO_CMD="--topo tree,depth=${DEPTH},fanout=${FANOUT}"
    ;;
  2)
    echo
    echo "🔗  You selected Linear Topology"
    read -p "   → Enter number of switches: " SWITCHES
    TOPO_CMD="--topo linear,${SWITCHES}"
    ;;
  3)
    echo
    echo "⚙️  You selected Single Switch Topology"
    read -p "   → Enter number of hosts: " HOSTS
    TOPO_CMD="--topo single,${HOSTS}"
    ;;
  *)
    echo
    echo "⚠️  Invalid choice — defaulting to Tree Topology (depth=2, fanout=3)"
    TOPO_CMD="--topo tree,depth=2,fanout=3"
    ;;
esac

echo
echo "✅ Using topology: ${TOPO_CMD}"
sleep 1

# --------------------------------------------------------------
# 🚀 Launch tmux environment
# --------------------------------------------------------------
tmux has-session -t "$SESSION" 2>/dev/null && tmux kill-session -t "$SESSION"
tmux new-session -d -s "$SESSION" -c "$PROJECT_DIR"

# Pane 1 — Ryu Controller
tmux send-keys -t "$SESSION":0.0 "
echo '🧠 [Ryu Controller] Starting on ${CTRL_IP}:${CTRL_OF_PORT} (REST at ${CTRL_REST_URL})'
ryu-manager --observe-links \
  ryu.app.simple_switch_13 \
  ryu.app.ofctl_rest \
  ryu.app.rest_topology \
  ryu.topology.switches \
  --ofp-tcp-listen-port ${CTRL_OF_PORT}
" C-m

# Pane 2 — Mininet
tmux split-window -h -t "$SESSION":0.0 -c "$PROJECT_DIR"
tmux send-keys -t "$SESSION":0.1 "
echo '🏗️ [Mininet] Launching topology → ${TOPO_CMD}'
sudo mn ${TOPO_CMD} \
  --controller=remote,ip=${CTRL_IP},port=${CTRL_OF_PORT} \
  --switch ovsk,protocols=OpenFlow13,failMode=secure
" C-m

# Pane 3 — Maven Java Client
tmux split-window -v -t "$SESSION":0.1 -c "$PROJECT_DIR"
tmux send-keys -t "$SESSION":0.2 "
echo '☕ [Java Client] Building & running RyuClient...'
mvn -DskipTests clean package
mvn exec:java -Dexec.mainClass='com.sdnmanager.RyuClient'
" C-m

# --------------------------------------------------------------
# ✅ Attach to session
# --------------------------------------------------------------
clear
echo "=============================================================="
echo "🚀 SDN DEMO STARTING"
echo "=============================================================="
echo "🧠 Pane 1 : Ryu Controller logs"
echo "🏗️ Pane 2 : Mininet (type 'pingall' to test connectivity)"
echo "☕ Pane 3 : Java Client (Options 1–7)"
echo "    → Option 7 launches the JavaFX Topology Visualization"
echo "--------------------------------------------------------------"
echo "🎯 Controls:"
echo "   - Detach from tmux: Ctrl + b then d"
echo "   - Reattach later  : tmux attach -t $SESSION"
echo "   - Kill session    : tmux kill-session -t $SESSION"
echo "=============================================================="
sleep 2
tmux attach -t "$SESSION"

