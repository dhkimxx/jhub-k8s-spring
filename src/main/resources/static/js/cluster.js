(() => {
  const root = document.querySelector('[data-page="cluster"]');
  if (!root) {
    return;
  }

  const refreshBtn = document.getElementById("refresh-cluster");
  const overviewWrapper = document.getElementById("cluster-overview");
  const overviewLoading = document.getElementById("cluster-overview-loading");
  const overviewError = document.getElementById("cluster-overview-error");
  const overviewAlert = document.getElementById("cluster-overview-alert");

  const overviewFields = {
    totalNodes: document.getElementById("overview-total-nodes"),
    readyNodes: document.getElementById("overview-ready-nodes"),
    totalSessions: document.getElementById("overview-total-sessions"),
    runningSessions: document.getElementById("overview-running-sessions"),
    cpuPercent: document.getElementById("overview-cpu-percent"),
    cpuDetail: document.getElementById("overview-cpu-detail"),
    memPercent: document.getElementById("overview-mem-percent"),
    memDetail: document.getElementById("overview-mem-detail"),
  };

  const nodesList = document.getElementById("nodes-list");
  const nodesLoading = document.getElementById("nodes-loading");
  const nodesError = document.getElementById("nodes-error");
  const nodesEmpty = document.getElementById("nodes-empty");

  let cpuChart = null;
  let memChart = null;

  const toggle = (el, show) => {
    if (!el) return;
    el.classList.toggle("hidden", !show);
  };

  const formatNumber = (value, unit = "") => {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return "-";
    }
    return `${Math.round(value * 10) / 10}${unit}`;
  };

  const formatBytes = (bytes) => {
    if (
      bytes === null ||
      bytes === undefined ||
      Number.isNaN(bytes) ||
      bytes === 0
    ) {
      return "0 B";
    }
    const units = ["B", "KB", "MB", "GB", "TB"];
    let i = 0;
    let value = bytes;
    while (value >= 1024 && i < units.length - 1) {
      value /= 1024;
      i++;
    }
    return `${Math.round(value * 10) / 10} ${units[i]}`;
  };

  const setOverviewAlert = (message) => {
    if (!overviewAlert) return;
    if (!message) {
      overviewAlert.textContent = "";
      toggle(overviewAlert, false);
      return;
    }
    overviewAlert.textContent = message;
    toggle(overviewAlert, true);
  };

  const createDoughnutChart = (canvasId, usedPercent, usedColor) => {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return null;
    const remaining = Math.max(0, 100 - usedPercent);
    return new Chart(ctx, {
      type: "doughnut",
      data: {
        datasets: [
          {
            data: [usedPercent, remaining],
            backgroundColor: [usedColor, "rgba(51, 65, 85, 0.5)"],
            borderWidth: 0,
          },
        ],
      },
      options: {
        cutout: "70%",
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { display: false },
          tooltip: { enabled: false },
        },
      },
    });
  };

  const renderOverview = (overview) => {
    overviewFields.totalNodes.textContent = overview.totalNodes;
    overviewFields.readyNodes.textContent = `Ready ${overview.readyNodes}/${overview.totalNodes}`;
    overviewFields.totalSessions.textContent = overview.totalSessions;
    overviewFields.runningSessions.textContent = `Running ${overview.runningSessions}/${overview.totalSessions}`;

    // CPU Chart
    const cpuPercent = overview.cpuUsagePercent || 0;
    overviewFields.cpuPercent.textContent = `${formatNumber(cpuPercent)}%`;
    overviewFields.cpuDetail.textContent = `${formatNumber(
      overview.totalCpuRequestedMilliCores,
      "m"
    )} / ${formatNumber(overview.totalCpuAllocatableMilliCores, "m")}`;
    if (cpuChart) cpuChart.destroy();
    cpuChart = createDoughnutChart("overview-cpu-chart", cpuPercent, "#10b981");

    // Memory Chart
    const memPercent = overview.memoryUsagePercent || 0;
    overviewFields.memPercent.textContent = `${formatNumber(memPercent)}%`;
    overviewFields.memDetail.textContent = `${formatBytes(
      overview.totalMemoryRequestedBytes
    )} / ${formatBytes(overview.totalMemoryAllocatableBytes)}`;
    if (memChart) memChart.destroy();
    memChart = createDoughnutChart("overview-mem-chart", memPercent, "#0ea5e9");

    toggle(overviewWrapper, true);
  };

  const loadOverview = async () => {
    toggle(overviewWrapper, false);
    toggle(overviewError, false);
    toggle(overviewLoading, true);
    setOverviewAlert(null);
    try {
      const res = await fetch("/api/cluster/overview");
      if (!res.ok) {
        throw new Error(`Overview fetch failed: ${res.status}`);
      }
      const data = await res.json();
      renderOverview(data);
    } catch (error) {
      console.error(error);
      toggle(overviewError, true);
      setOverviewAlert("클러스터 개요를 가져오는 중 오류가 발생했습니다.");
    } finally {
      toggle(overviewLoading, false);
    }
  };

  const renderNodes = (nodes) => {
    nodesList.innerHTML = "";
    nodes.forEach((node) => {
      const card = document.createElement("div");
      const statusColor =
        node.status === "Ready" ? "text-emerald-300" : "text-rose-300";
      card.className =
        "rounded-2xl border border-slate-800 bg-slate-900/60 p-4 flex flex-col gap-4";
      card.innerHTML = `
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-xs text-slate-400">Node</p>
                        <p class="text-lg font-semibold">${node.nodeName}</p>
                    </div>
                    <span class="${statusColor} text-sm">${node.status}</span>
                </div>
                <div class="grid grid-cols-2 gap-2 text-xs text-slate-400">
                    <div>
                        <p>Pods</p>
                        <p class="text-slate-100">${node.runningPodCount}</p>
                    </div>
                    <div>
                        <p>Kubelet</p>
                        <p class="text-slate-100">${node.kubeletVersion}</p>
                    </div>
                    <div class="col-span-2">
                        <p class="text-slate-400">CPU (m)</p>
                        <p class="text-slate-100">${formatNumber(
                          node.requestedCpuMilliCores,
                          "m"
                        )} / Alloc ${formatNumber(
        node.allocatableCpuMilliCores,
        "m"
      )}</p>
                    </div>
                    <div class="col-span-2">
                        <p class="text-slate-400">Memory</p>
                        <p class="text-slate-100">${formatBytes(
                          node.requestedMemoryBytes
                        )} / Alloc ${formatBytes(
        node.allocatableMemoryBytes
      )}</p>
                    </div>
                </div>
            `;
      nodesList.appendChild(card);
    });
  };

  const loadNodes = async () => {
    toggle(nodesLoading, true);
    toggle(nodesError, false);
    toggle(nodesEmpty, false);
    nodesList.innerHTML = "";
    setOverviewAlert(null);
    try {
      const res = await fetch("/api/cluster/nodes");
      if (!res.ok) {
        throw new Error(`Nodes fetch failed: ${res.status}`);
      }
      const data = await res.json();
      if (!data.length) {
        toggle(nodesEmpty, true);
      } else {
        renderNodes(data);
      }
    } catch (error) {
      console.error(error);
      toggle(nodesError, true);
      setOverviewAlert("노드 목록을 가져오는 중 오류가 발생했습니다.");
    } finally {
      toggle(nodesLoading, false);
    }
  };

  const refreshAll = () => {
    loadOverview();
    loadNodes();
  };

  refreshBtn?.addEventListener("click", refreshAll);
  refreshAll();
})();
