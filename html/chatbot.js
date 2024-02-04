function createChart(table, rowLabels, columnLabels, chartType, title, colorPalette) {
  // Create a canvas to draw the chart on
  var canvas = document.createElement('canvas');
  $('#chatBox').append(canvas);
  let indexAxis = 'x';
  if (chartType=='bar') {
    indexAxis = 'y';
  } else if (chartType=='column') {
    chartType = 'bar';
  }

  let rowColors = table.length>1;
  console.log()
  // Prepare the dataset
  var datasets = table.map((row, rowIndex) => {
    return {
      label: rowLabels[rowIndex],
      data: row,
      fill: false,
      backgroundColor: rowColors ? colorPalette[rowIndex%colorPalette.length] : getColors(colorPalette, row.length),
      borderColor: 'rgba(0,0,0,1)',
      borderWidth: 1,
      tension: 0.1 // This is for line chart
    };
  });

  // Create the chart
  var ctx = canvas.getContext('2d');
  var chart = new Chart(ctx, {
    type: chartType,
    data: {
      labels: columnLabels,
      datasets: datasets
    },
    options: {
      indexAxis: indexAxis,
      responsive: true,
      plugins: {
        legend: {
          display: rowColors,
          position: 'top'
        },
        title: {
          display: true,
          text: title
        }
      }
    }
  });
}

function getColors(colors, length) {
  if (length <= colors.length) {
    // If length is smaller or equal to the length of colors,
    // return a slice of the array
    return colors.slice(0, length);
  } else {
    // If length is larger than the length of colors,
    // copy elements from the colors array to fill the rest
    let result = [...colors];
    while (result.length < length) {
      result = result.concat(colors.slice(0, length - result.length));
    }
    return result;
  }
}



let colorPalette = [ // Tableau 20
  'rgba(31, 119, 180, 1)',
  'rgba(174, 199, 232, 1)',
  'rgba(255, 127, 14, 1)',
  'rgba(255, 187, 120, 1)',
  'rgba(44, 160, 44, 1)',
  'rgba(152, 223, 138, 1)',
  'rgba(214, 39, 40, 1)',
  'rgba(255, 152, 150, 1)',
  'rgba(148, 103, 189, 1)',
  'rgba(197, 176, 213, 1)',
  'rgba(140, 86, 75, 1)',
  'rgba(196, 156, 148, 1)',
  'rgba(227, 119, 194, 1)',
  'rgba(247, 182, 210, 1)',
  'rgba(127, 127, 127, 1)',
  'rgba(199, 199, 199, 1)',
  'rgba(188, 189, 34, 1)',
  'rgba(219, 219, 141, 1)',
  'rgba(23, 190, 207, 1)',
  'rgba(158, 218, 229, 1)'
];

function chartData(chart) {
  let transpose = chart.table.length > chart.table[0].length;
  console.log(transpose);
  let table, rowLabels,columnLabels;
  if (transpose) {
    table = chart.table[0].map((_, colIndex) => chart.table.map(row => row[colIndex]));
    rowLabels = chart.columnLabels;
    columnLabels = chart.rowLabels;
  } else {
    table = chart.table;
    rowLabels = chart.rowLabels;
    columnLabels = chart.columnLabels;
  }
  createChart(table, rowLabels, columnLabels, chart.chartType, chart.title, colorPalette);
}