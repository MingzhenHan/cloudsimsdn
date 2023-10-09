import React, { useEffect, useState } from 'react';
import qs from 'qs';
import {Button, Table} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import type { FilterValue, SorterResult } from 'antd/es/table/interface';
import {postRequest} from "./request";

interface DataType {
  jobid: number;
  vmid: string;
  status: string;
  starttime: string;
  finishtime: string;
  time: string;
}

interface TableParams {
  pagination?: TablePaginationConfig;
  action?:string,
  field?: string;
  order?: string;
  filters?: Record<string, FilterValue>;
}

const columns: ColumnsType<DataType> = [
  {
    title: '请求序号',
    dataIndex: 'jobid',
    // sorter: true,
    width: '10%',
  },
  {
    title: '发送端ID',
    dataIndex: 'vmid',
    width: '10%',
  },
  {
    title: '结果状态',
    dataIndex: 'status',
    width: '20%',
  },
  {
    title: '发送时间',
    dataIndex: 'starttime',
    width: '20%',
  },
  {
    title: '到达时间',
    dataIndex: 'finishtime',
    width: '20%',
  },
  {
    title: '时延',
    dataIndex: 'time',
    // width: '20%',
  },
];

const App: React.FC = () => {
  const [data, setData] = useState<DataType[]>();
  const [loading, setLoading] = useState(false);
  const [tableParams, setTableParams] = useState<TableParams>({
    pagination: {
      current: 1,
      pageSize: 10,
    },
  });

  const getData = () => {
    console.log("getData被调用")
    setLoading(true);

    postRequest(`http://localhost:8082/run`, JSON.stringify("get results"), (res:any)=>{
      setData(res.data);
      console.log(res)
      setLoading(false);
      setTableParams({
        ...tableParams,
        pagination: {
          ...tableParams.pagination,
          total: 200,
          // 200 is mock data, you should read it from server
          // total: data.totalCount,
        },
      });
    })
  };



  const handleTableChange = (pagination: any, filters: any, sorter: any, extra: any) => {
    console.log("onChange triggered!");
    setTableParams({
      pagination,
      filters,
      ...sorter,
      ...extra
    });
    // `dataSource` is useless since `pageSize` changed
    if (pagination.pageSize !== tableParams.pagination?.pageSize) {
      setData([]);
    }
  };
  // const handleTableChange = (pagination: any, filters: any, sorter: any, extra: any) => {
  //   console.log('params\n', pagination, filters, sorter, extra);
  // };


  return (
      <>
        <Button onClick={getData} >开始仿真</Button>
        <Table
            columns={columns}
            rowKey={record => record.jobid}
            dataSource={data}
            pagination={false}
            // loading={loading}
            // onChange={false}

        />
      </>
  );
};

export default App;
