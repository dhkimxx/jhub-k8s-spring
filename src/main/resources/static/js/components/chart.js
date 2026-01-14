/**
 * 공통 차트 컴포넌트
 * Doughnut 차트 생성 및 관리
 */

/**
 * Doughnut 차트 생성
 * @param {string} canvasId - 캔버스 요소 ID
 * @param {number} usedPercent - 사용률 (0-100)
 * @param {string} usedColor - 사용된 부분의 색상 (hex)
 * @returns {Chart|null} Chart.js 인스턴스
 */
export const createDoughnutChart = (canvasId, usedPercent, usedColor) => {
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
        tooltip: { enabled: true },
      },
    },
  });
};
