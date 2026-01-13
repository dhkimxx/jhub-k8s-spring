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
    cpuRequested: document.getElementById("overview-cpu-requested"),
    cpuCapacity: document.getElementById("overview-cpu-capacity"),
    memRequested: document.getElementById("overview-mem-requested"),
    memCapacity: document.getElementById("overview-mem-capacity"),
  };

  const nodesList = document.getElementById("nodes-list");
  const nodesLoading = document.getElementById("nodes-loading");
  const nodesError = document.getElementById("nodes-error");
  const nodesEmpty = document.getElementById("nodes-empty");

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

  const renderOverview = (overview) => {
    overviewFields.totalNodes.textContent = overview.totalNodes;
    overviewFields.readyNodes.textContent = `Ready ${overview.readyNodes}/${overview.totalNodes}`;
    overviewFields.totalSessions.textContent = overview.totalSessions;
    overviewFields.runningSessions.textContent = `Running ${overview.runningSessions}/${overview.totalSessions}`;
    overviewFields.cpuRequested.textContent = formatNumber(
      overview.totalCpuRequestedMilliCores,
      "m"
    );
    overviewFields.cpuCapacity.textContent = `Alloc ${formatNumber(
      overview.totalCpuAllocatableMilliCores,
      "m"
    )} / Cap ${formatNumber(overview.totalCpuCapacityMilliCores, "m")}`;
    overviewFields.memRequested.textContent = formatNumber(
      overview.totalMemoryRequestedMiB,
      "MiB"
    );
    overviewFields.memCapacity.textContent = `Alloc ${formatNumber(
      overview.totalMemoryAllocatableMiB,
      "MiB"
    )} / Cap ${formatNumber(overview.totalMemoryCapacityMiB, "MiB")}`;
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
                        <p class="text-slate-400">Memory (MiB)</p>
                        <p class="text-slate-100">${formatNumber(
                          node.requestedMemoryMiB,
                          "MiB"
                        )} / Alloc ${formatNumber(
        node.allocatableMemoryMiB,
        "MiB"
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
