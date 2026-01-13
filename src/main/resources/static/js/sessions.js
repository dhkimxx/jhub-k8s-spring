(() => {
  const root = document.querySelector('[data-page="sessions"]');
  if (!root) {
    return;
  }

  const listContainer = document.getElementById("sessions-list");
  const loadingEl = document.getElementById("sessions-loading");
  const emptyEl = document.getElementById("sessions-empty");
  const errorEl = document.getElementById("sessions-error");
  const alertEl = document.getElementById("sessions-alert");

  const detailWrapper = document.getElementById("session-detail");
  const detailEmpty = document.getElementById("session-detail-empty");
  const detailLoading = document.getElementById("session-detail-loading");
  const detailError = document.getElementById("session-detail-error");
  const terminateBtn = document.getElementById("terminate-session");

  const detailFields = {
    username: document.getElementById("detail-username"),
    pod: document.getElementById("detail-pod"),
    node: document.getElementById("detail-node"),
    phase: document.getElementById("detail-phase"),
    start: document.getElementById("detail-start"),
    requestCpu: document.getElementById("detail-request-cpu"),
    usageCpu: document.getElementById("detail-usage-cpu"),
    requestMem: document.getElementById("detail-request-mem"),
    usageMem: document.getElementById("detail-usage-mem"),
  };
  const pvcSection = document.getElementById("session-pvc-section");
  const eventsList = document.getElementById("session-events");

  let selectedUsername = null;
  let cpuChart = null;
  let memChart = null;

  const initChart = (ctx, label, color) => {
    return new Chart(ctx, {
      type: "doughnut",
      data: {
        labels: ["Used", "Free"],
        datasets: [
          {
            data: [0, 100],
            backgroundColor: [color, "rgba(71, 85, 105, 0.3)"], // slate-600 with opacity
            borderWidth: 0,
            cutout: "75%",
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { display: false },
          tooltip: { enabled: false },
        },
        animation: { duration: 0 },
      },
    });
  };

  const updateChartData = (chart, usage, limit) => {
    if (!limit || limit === 0) {
      // Limit이 없으면 꽉 찬 원으로 표시하되 색상을 다르게 하거나...
      // 여기서는 그냥 0%로 처리하거나 100%로 처리.
      // Limit이 없으면 (Unbounded), Usage만 보여주는 건 의미가 모호함.
      // 일단 Free를 0으로 하고 전체를 회색으로? 아니면 Usage만큼 채우기?
      // Limit이 없을 땐 차트를 비활성화하거나 "No Limit" 표시가 나음.
      // 여기서는 심플하게 100% Free로 표시하거나,
      // Usage가 있으면 Usage만큼만 보여주기?(비율 불가능)
      // 비율 계산 불가시 : [0, 1] (Empty)
      chart.data.datasets[0].data = [0, 1];
    } else {
      const percentage = Math.min((usage / limit) * 100, 100);
      chart.data.datasets[0].data = [percentage, 100 - percentage];
    }
    chart.update();
  };

  const toggle = (el, show) => {
    if (!el) return;
    el.classList.toggle("hidden", !show);
  };

  const formatDate = (value) => {
    if (!value) return "-";
    return new Intl.DateTimeFormat("ko-KR", {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(new Date(value));
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

  const setAlert = (message) => {
    if (!alertEl) return;
    if (!message) {
      alertEl.textContent = "";
      toggle(alertEl, false);
      return;
    }
    alertEl.textContent = message;
    toggle(alertEl, true);
  };

  const renderSessions = (sessions) => {
    listContainer.innerHTML = "";
    sessions.forEach((session) => {
      const card = document.createElement("button");
      card.type = "button";
      card.className =
        "rounded-2xl border border-slate-800 bg-slate-900/60 p-4 text-left transition hover:border-pink-400";
      card.dataset.username = session.username;
      card.innerHTML = `
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-xs text-slate-400">${
                          session.namespace
                        }</p>
                        <p class="text-lg font-semibold">${session.username}</p>
                    </div>
                    <span class="text-sm ${
                      session.ready ? "text-emerald-300" : "text-rose-300"
                    }">
                        ${session.phase}
                    </span>
                </div>
                <dl class="mt-4 grid grid-cols-2 gap-3 text-xs text-slate-400">
                    <div>
                        <dt>Node</dt>
                        <dd class="text-slate-100">${
                          session.nodeName ?? "-"
                        }</dd>
                    </div>
                    <div>
                        <dt>Restart</dt>
                        <dd class="text-slate-100">${session.restartCount}</dd>
                    </div>
                    <div>
                        <dt>CPU Req</dt>
                        <dd class="text-slate-100">${formatNumber(
                          session.cpuMilliCores,
                          "m"
                        )}</dd>
                    </div>
                    <div>
                        <dt>Mem Req</dt>
                        <dd class="text-slate-100">${formatBytes(
                          session.memoryBytes
                        )}</dd>
                    </div>
                </dl>
            `;
      card.addEventListener("click", () => {
        selectedUsername = session.username;
        loadSessionDetail(session.username);
      });
      listContainer.appendChild(card);
    });
  };

  const loadSessions = async () => {
    toggle(loadingEl, true);
    toggle(emptyEl, false);
    toggle(errorEl, false);
    setAlert(null);
    try {
      const res = await fetch("/api/sessions");
      if (!res.ok) {
        throw new Error(`세션 조회 실패: ${res.status}`);
      }
      const data = await res.json();
      if (!data.length) {
        toggle(emptyEl, true);
      } else {
        renderSessions(data);
      }
    } catch (error) {
      console.error(error);
      toggle(errorEl, true);
      setAlert("세션 목록을 가져오는 중 문제가 발생했습니다.");
    } finally {
      toggle(loadingEl, false);
    }
  };

  const renderDetail = (detail) => {
    toggle(detailWrapper, true);
    toggle(detailEmpty, false);
    toggle(detailError, false);

    // Metadata
    detailFields.username.textContent = detail.metadata.username;
    detailFields.pod.textContent = detail.metadata.podName;
    detailFields.node.textContent = detail.metadata.nodeName;

    // Status
    detailFields.phase.textContent = detail.status.phase;
    detailFields.start.textContent = formatDate(detail.status.startTime);

    // Resources (Request / Usage) & Charts
    if (detail.resources && detail.resources.cpu) {
      const cpu = detail.resources.cpu;
      detailFields.requestCpu.textContent = formatNumber(cpu.request, "m");
      detailFields.usageCpu.textContent = formatNumber(cpu.usage, "m");

      if (!cpuChart) {
        const ctx = document.getElementById("chart-cpu").getContext("2d");
        cpuChart = initChart(ctx, "CPU", "#f472b6"); // pink-400
      }
      updateChartData(cpuChart, cpu.usage, cpu.limit);
    } else {
      detailFields.requestCpu.textContent = "-";
      detailFields.usageCpu.textContent = "-";
      if (cpuChart) updateChartData(cpuChart, 0, 0);
    }

    if (detail.resources && detail.resources.memory) {
      const mem = detail.resources.memory;
      detailFields.requestMem.textContent = formatBytes(mem.request);
      detailFields.usageMem.textContent = formatBytes(mem.usage);

      if (!memChart) {
        const ctx = document.getElementById("chart-mem").getContext("2d");
        memChart = initChart(ctx, "MEM", "#34d399"); // emerald-400
      }
      updateChartData(memChart, mem.usage, mem.limit);
    } else {
      detailFields.requestMem.textContent = "-";
      detailFields.usageMem.textContent = "-";
      if (memChart) updateChartData(memChart, 0, 0);
    }

    eventsList.innerHTML = "";
    if (!detail.events.length) {
      const li = document.createElement("li");
      li.className = "text-slate-500";
      li.textContent = "최근 이벤트가 없습니다.";
      eventsList.appendChild(li);
    } else {
      detail.events.forEach((event) => {
        const li = document.createElement("li");
        li.className =
          "rounded-xl border border-slate-800/60 bg-slate-900/40 p-3 flex items-start gap-3";
        li.innerHTML = `
                    <span class="text-xs text-${
                      event.type === "Warning" ? "rose" : "emerald"
                    }-300">
                        ${event.type}
                    </span>
                    <div class="text-sm">
                        <p class="font-semibold">${
                          event.reason ?? "Unknown reason"
                        }</p>
                        <p class="text-slate-400">${event.message ?? ""}</p>
                        <p class="text-xs text-slate-500 mt-1">${formatDate(
                          event.eventTime
                        )}</p>
                    </div>
                `;
        eventsList.appendChild(li);
      });
    }

    // Storage 정보 렌더링
    if (detail.resources.storage && detail.resources.storage.type !== "NONE") {
      const s = detail.resources.storage;
      let content = "";
      if (s.type === "PVC") {
        content = `
                <div class="flex justify-between"><dt class="text-slate-500">스토리지 타입</dt><dd>PVC</dd></div>
                <div class="flex justify-between"><dt class="text-slate-500">PVC 이름</dt><dd>${
                  s.pvcName || "-"
                }</dd></div>
                <div class="flex justify-between"><dt class="text-slate-500">할당 용량</dt><dd>${formatBytes(
                  s.capacityBytes
                )}</dd></div>
                <div class="flex justify-between"><dt class="text-slate-500">요청 용량</dt><dd>${formatBytes(
                  s.requestBytes
                )}</dd></div>
                <div class="flex justify-between"><dt class="text-slate-500">StorageClass</dt><dd>${
                  s.storageClassName || "-"
                }</dd></div>
            `;
      } else if (s.type === "EPHEMERAL") {
        content = `
                <div class="flex justify-between"><dt class="text-slate-500">스토리지 타입</dt><dd>Ephemeral (임시)</dd></div>
                <div class="flex justify-between"><dt class="text-slate-500">용량 제한 (Limit)</dt><dd>${formatBytes(
                  s.capacityBytes
                )}</dd></div>
            `;
      }

      // 제목 업데이트
      const titleEl = pvcSection.querySelector("p");
      if (titleEl) titleEl.textContent = `Storage (${s.type})`;

      // DL 업데이트
      const dlEl = pvcSection.querySelector("dl");
      if (dlEl) dlEl.innerHTML = content;

      toggle(pvcSection, true);
    } else {
      toggle(pvcSection, false);
    }

    terminateBtn.dataset.pod = detail.metadata.podName;
    toggle(terminateBtn, true);
  };

  const loadSessionDetail = async (username) => {
    toggle(detailWrapper, false);
    toggle(detailError, false);
    toggle(detailEmpty, false);
    toggle(detailLoading, true);
    setAlert(null);

    try {
      const res = await fetch(`/api/sessions/${encodeURIComponent(username)}`);
      if (!res.ok) {
        throw new Error(`세션 상세 조회 실패: ${res.status}`);
      }
      const detail = await res.json();
      renderDetail(detail);
    } catch (error) {
      console.error(error);
      toggle(detailError, true);
      setAlert("세션 상세를 가져오는 중 문제가 발생했습니다.");
    } finally {
      toggle(detailLoading, false);
    }
  };

  const terminateSession = async () => {
    const podName = terminateBtn.dataset.pod;
    if (!podName) return;
    if (!confirm("해당 세션을 종료하시겠습니까?")) {
      return;
    }
    setAlert("세션 종료 요청을 전송했습니다. 잠시 후 목록을 새로고침하세요.");
    try {
      const res = await fetch(`/api/sessions/${encodeURIComponent(podName)}`, {
        method: "DELETE",
      });
      if (!res.ok && res.status !== 202) {
        throw new Error(`세션 종료 실패: ${res.status}`);
      }
      await loadSessions();
    } catch (error) {
      console.error(error);
      setAlert("세션 종료 요청이 실패했습니다.");
    }
  };

  document
    .getElementById("refresh-sessions")
    ?.addEventListener("click", loadSessions);
  terminateBtn?.addEventListener("click", terminateSession);
  loadSessions();
})();
