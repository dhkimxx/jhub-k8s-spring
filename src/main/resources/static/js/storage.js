(() => {
  const root = document.querySelector('[data-page="storage"]');
  if (!root) {
    return;
  }

  const loadingEl = document.getElementById("storage-loading");
  const errorEl = document.getElementById("storage-error");
  const alertEl = document.getElementById("storage-alert");
  const overviewWrapper = document.getElementById("storage-overview");
  const pvListEl = document.getElementById("pv-list");
  const pvEmptyEl = document.getElementById("pv-empty");
  const pvcListEl = document.getElementById("pvc-list");
  const pvcEmptyEl = document.getElementById("pvc-empty");

  const overviewFields = {
    totalPv: document.getElementById("overview-total-pv"),
    boundPv: document.getElementById("overview-bound-pv"),
    totalCapacity: document.getElementById("overview-total-capacity"),
    totalPvc: document.getElementById("overview-total-pvc"),
  };

  const toggle = (el, show) => {
    if (!el) return;
    el.classList.toggle("hidden", !show);
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

  const getPhaseColor = (phase) => {
    switch (phase) {
      case "Bound":
        return "text-emerald-400";
      case "Available":
        return "text-sky-400";
      case "Released":
        return "text-amber-400";
      case "Pending":
        return "text-yellow-400";
      case "Failed":
      case "Lost":
        return "text-rose-400";
      default:
        return "text-slate-400";
    }
  };

  const renderOverview = (data) => {
    overviewFields.totalPv.textContent = data.totalPvCount;
    overviewFields.boundPv.textContent = `${data.boundPvCount} / ${data.totalPvCount}`;
    overviewFields.totalCapacity.textContent = formatBytes(
      data.totalCapacityBytes
    );
    overviewFields.totalPvc.textContent = data.totalPvcCount;
    toggle(overviewWrapper, true);
  };

  const renderPvList = (pvList) => {
    pvListEl.innerHTML = "";
    if (!pvList || pvList.length === 0) {
      toggle(pvEmptyEl, true);
      return;
    }
    toggle(pvEmptyEl, false);

    pvList.forEach((pv) => {
      const card = document.createElement("div");
      card.className =
        "rounded-2xl border border-slate-800 bg-slate-900/60 p-4 flex flex-col gap-3";
      card.innerHTML = `
        <div class="flex items-center justify-between">
          <p class="text-lg font-semibold truncate">${pv.pvName}</p>
          <span class="${getPhaseColor(pv.phase)} text-sm">${pv.phase}</span>
        </div>
        <dl class="grid grid-cols-2 gap-2 text-xs text-slate-400">
          <div>
            <dt>용량</dt>
            <dd class="text-slate-100">${formatBytes(pv.capacityBytes)}</dd>
          </div>
          <div>
            <dt>StorageClass</dt>
            <dd class="text-slate-100">${pv.storageClassName || "-"}</dd>
          </div>
          <div>
            <dt>Reclaim Policy</dt>
            <dd class="text-slate-100">${pv.reclaimPolicy || "-"}</dd>
          </div>
          <div>
            <dt>Claim</dt>
            <dd class="text-slate-100 truncate">${pv.claimRef || "-"}</dd>
          </div>
        </dl>
      `;
      pvListEl.appendChild(card);
    });
  };

  const renderPvcList = (pvcList) => {
    pvcListEl.innerHTML = "";
    if (!pvcList || pvcList.length === 0) {
      toggle(pvcEmptyEl, true);
      return;
    }
    toggle(pvcEmptyEl, false);

    pvcList.forEach((pvc) => {
      const card = document.createElement("div");
      card.className =
        "rounded-2xl border border-slate-800 bg-slate-900/60 p-4 flex flex-col gap-3";
      card.innerHTML = `
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-slate-400">${pvc.namespace}</p>
            <p class="text-lg font-semibold truncate">${pvc.pvcName}</p>
          </div>
          <span class="${getPhaseColor(pvc.phase)} text-sm">${pvc.phase}</span>
        </div>
        <dl class="grid grid-cols-2 gap-2 text-xs text-slate-400">
          <div>
            <dt>용량</dt>
            <dd class="text-slate-100">${formatBytes(pvc.capacityBytes)}</dd>
          </div>
          <div>
            <dt>StorageClass</dt>
            <dd class="text-slate-100">${pvc.storageClassName || "-"}</dd>
          </div>
          <div>
            <dt>Access Modes</dt>
            <dd class="text-slate-100">${
              pvc.accessModes?.join(", ") || "-"
            }</dd>
          </div>
          <div>
            <dt>Volume</dt>
            <dd class="text-slate-100 truncate">${pvc.volumeName || "-"}</dd>
          </div>
        </dl>
      `;
      pvcListEl.appendChild(card);
    });
  };

  const loadStorage = async () => {
    toggle(loadingEl, true);
    toggle(errorEl, false);
    toggle(overviewWrapper, false);
    setAlert(null);

    try {
      const res = await fetch("/api/storage/overview");
      if (!res.ok) {
        throw new Error(`스토리지 조회 실패: ${res.status}`);
      }
      const data = await res.json();
      renderOverview(data);
      renderPvList(data.pvList);
      renderPvcList(data.pvcList);
    } catch (error) {
      console.error(error);
      toggle(errorEl, true);
      setAlert("스토리지 정보를 가져오는 중 문제가 발생했습니다.");
    } finally {
      toggle(loadingEl, false);
    }
  };

  document
    .getElementById("refresh-storage")
    ?.addEventListener("click", loadStorage);
  loadStorage();
})();
