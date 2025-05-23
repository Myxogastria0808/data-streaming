use crate::{
    error::window::WindowError,
    model::window_data::WindowData,
    utils::{args::ArgsSet, stat},
};
use std::collections::VecDeque;

pub fn count_window(
    is_first_flag: &mut bool,
    stock_data_buffer: &mut VecDeque<WindowData>,
    args_set: &ArgsSet,
    window_data: WindowData,
) -> Result<(), WindowError> {
    if *is_first_flag {
        // first window process
        if stock_data_buffer.len() < args_set.get_window_count_value()? as usize {
            // push back WindowData
            stock_data_buffer.push_back(window_data);
        } else {
            *is_first_flag = false;
        }
        if !*is_first_flag {
            // show result
            stat::show_stat(stock_data_buffer.clone(), args_set.clone())?;
            // pop over record
            for _ in 0..args_set.get_slide_count_value()? {
                // pop first element
                stock_data_buffer.pop_front();
            }
        }
    } else {
        // other window process
        if stock_data_buffer.len() < args_set.get_window_count_value()? as usize {
            // push back WindowData
            stock_data_buffer.push_back(window_data);
        } else {
            // show result
            stat::show_stat(stock_data_buffer.clone(), args_set.clone())?;
            // pop over record
            for _ in 0..args_set.get_slide_count_value()? {
                // pop first element
                stock_data_buffer.pop_front();
            }
        }
    }
    Ok(())
}
