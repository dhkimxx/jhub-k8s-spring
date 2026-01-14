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
    storagePercent: document.getElementById("overview-storage-percent"),
    storageDetail: document.getElementById("overview-storage-detail"),
  };

  const nodesList = document.getElementById("nodes-list");
  const nodesLoading = document.getElementById("nodes-loading");
  const nodesError = document.getElementById("nodes-error");
  const nodesEmpty = document.getElementById("nodes-empty");

  let cpuChart = null;
  let memChart = null;
  let storageChart = null;

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

    // Storage Chart
    const storagePercent = overview.ephemeralStorageUsagePercent || 0;
    overviewFields.storagePercent.textContent = `${formatNumber(
      storagePercent
    )}%`;
    overviewFields.storageDetail.textContent = `${formatBytes(
      overview.totalEphemeralStorageRequestedBytes
    )} / ${formatBytes(overview.totalEphemeralStorageAllocatableBytes)}`;
    if (storageChart) storageChart.destroy();
    storageChart = createDoughnutChart(
      "overview-storage-chart",
      storagePercent,
      "#f59e0b" // amber-500
    );

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

  let nodeCharts = []; // Store chart instances to destroy them later

  const renderNodes = (nodes) => {
    // Clean up old charts
    nodeCharts.forEach((chart) => chart.destroy());
    nodeCharts = [];
    nodesList.innerHTML = "";

    nodes.forEach((node, index) => {
      const card = document.createElement("div");
      const statusColor =
        node.status === "Ready" ? "text-emerald-300" : "text-rose-300";
      card.className =
        "rounded-2xl border border-slate-800 bg-slate-900/60 p-6 flex flex-col gap-6";

      // Unique IDs for canvases
      const cpuChartId = `node-cpu-chart-${index}`;
      const memChartId = `node-mem-chart-${index}`;
      const storageChartId = `node-storage-chart-${index}`;

      card.innerHTML = `
                <div class="flex items-center justify-between border-b border-slate-800 pb-4">
                    <div>
                        <div class="flex items-center gap-2">
                            <span class="text-xs text-slate-400">Node</span>
                            <span class="${statusColor} text-xs border border-slate-700 px-2 py-0.5 rounded-full bg-slate-800">${
        node.status
      }</span>
                        </div>
                        <p class="text-lg font-semibold mt-1">${
                          node.nodeName
                        }</p>
                    </div>
                     <div class="text-right">
                        <p class="text-xs text-slate-400">Pods</p>
                        <p class="text-slate-100 font-medium">${
                          node.runningPodCount
                        }</p>
                    </div>
                </div>

                <div class="grid grid-cols-3 gap-2">
                    <!-- CPU -->
                    <div class="flex flex-col items-center gap-2">
                        <p class="text-[10px] text-slate-400 uppercase">CPU</p>
                        <div class="relative w-20 h-20">
                            <canvas id="${cpuChartId}"></canvas>
                             <div class="absolute inset-0 flex items-center justify-center">
                                <span class="text-xs font-medium text-slate-300">${Math.round(
                                  node.cpuUsagePercent
                                )}%</span>
                            </div>
                        </div>
                        <p class="text-[10px] text-slate-500">${formatNumber(
                          node.requestedCpuMilliCores,
                          "m"
                        )} / ${formatNumber(
        node.allocatableCpuMilliCores,
        "m"
      )}</p>
                    </div>

                    <!-- Memory -->
                    <div class="flex flex-col items-center gap-2">
                        <p class="text-[10px] text-slate-400 uppercase">Memory</p>
                        <div class="relative w-20 h-20">
                            <canvas id="${memChartId}"></canvas>
                             <div class="absolute inset-0 flex items-center justify-center">
                                <span class="text-xs font-medium text-slate-300">${Math.round(
                                  node.memoryUsagePercent
                                )}%</span>
                            </div>
                        </div>
                        <p class="text-[10px] text-slate-500">${formatBytes(
                          node.requestedMemoryBytes
                        )}</p>
                    </div>

                    <!-- Disk -->
                    <div class="flex flex-col items-center gap-2">
                        <p class="text-[10px] text-slate-400 uppercase">Disk</p>
                        <div class="relative w-20 h-20">
                            <canvas id="${storageChartId}"></canvas>
                             <div class="absolute inset-0 flex items-center justify-center">
                                <span class="text-xs font-medium text-slate-300">${Math.round(
                                  node.ephemeralStorageUsagePercent
                                )}%</span>
                            </div>
                        </div>
                         <p class="text-[10px] text-slate-500">${formatBytes(
                           node.requestedEphemeralStorageBytes
                         )}</p>
                    </div>
                </div>
                
                <div class="grid grid-cols-2 gap-2 text-xs text-slate-500 border-t border-slate-800 pt-4 mt-auto">
                   <div>
                        <p>Kubelet</p>
                        <p class="text-slate-300">${node.kubeletVersion}</p>
                    </div>
                    <div>
                        <p>OS</p>
                        <p class="text-slate-300">${node.osImage}</p>
                    </div>
                </div>
            `;
      nodesList.appendChild(card);

      // Initialize Charts
      nodeCharts.push(
        createDoughnutChart(cpuChartId, node.cpuUsagePercent, "#10b981")
      );
      nodeCharts.push(
        createDoughnutChart(memChartId, node.memoryUsagePercent, "#0ea5e9")
      );
      nodeCharts.push(
        createDoughnutChart(
          storageChartId,
          node.ephemeralStorageUsagePercent,
          "#f59e0b"
        )
      );
    });
  };

  const loadNodes = async () => {
    toggle(nodesLoading, true);
    toggle(nodesError, false);
    toggle(nodesEmpty, false);
    // nodesList.innerHTML = ""; // Handled in renderNodes to keep layout stable until load? No, clear it in renderNodes is fine.
    setOverviewAlert(null);
    try {
      const res = await fetch("/api/cluster/nodes");
      if (!res.ok) {
        throw new Error(`Nodes fetch failed: ${res.status}`);
      }
      const data = await res.json();
      if (!data.length) {
        toggle(nodesEmpty, true);
        nodesList.innerHTML = ""; // Clear if empty
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
